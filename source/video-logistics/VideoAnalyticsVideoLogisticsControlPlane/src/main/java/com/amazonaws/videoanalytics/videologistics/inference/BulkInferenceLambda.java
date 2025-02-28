package com.amazonaws.videoanalytics.videologistics.inference;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.INVALID_INPUT_EXCEPTION;
import static com.amazonaws.videoanalytics.videologistics.utils.AWSVideoAnalyticsServiceLambdaConstants.UPLOAD_BUCKET_FORMAT;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse.BatchItemFailure;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClient;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientProvider;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import com.amazonaws.videoanalytics.videologistics.dagger.AWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.dagger.DaggerAWSVideoAnalyticsVLControlPlaneComponent;
import com.amazonaws.videoanalytics.videologistics.utils.InferenceUtils;
import com.amazonaws.videoanalytics.videologistics.utils.annotations.ExcludeFromJacocoGeneratedReport;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import software.amazon.awssdk.regions.Region;

public class BulkInferenceLambda implements RequestHandler<KinesisEvent, StreamsEventResponse> {
    private static final Logger LOG = LogManager.getLogger(BulkInferenceLambda.class);
    private final OpenSearchClientProvider openSearchClientProvider;
    private final InferenceSerializer serializer;
    private final InferenceDeserializer deserializer;
    private final Region region;
    private final ThumbnailS3PresignerFactory presignerFactory;
    private final ImageUploader imageUploader;
    private final String accountId;
    private final String endpoint = System.getProperty("opensearchEndpoint", System.getenv("opensearchEndpoint"));

    @ExcludeFromJacocoGeneratedReport
    public BulkInferenceLambda() {
        AWSVideoAnalyticsVLControlPlaneComponent component = DaggerAWSVideoAnalyticsVLControlPlaneComponent.create();
        component.inject(this);
        openSearchClientProvider = component.getOpenSearchClientProvider();
        serializer = component.getInferenceSerializer();
        deserializer = component.getInferenceDeserializer();
        region = component.getRegion();
        presignerFactory = component.getThumbnailS3PresignerFactory();
        imageUploader = component.getImageUploader();
        accountId = component.getAccountId();
        }

    @Inject
    public BulkInferenceLambda(final OpenSearchClientProvider openSearchClientProvider,
                               final InferenceSerializer serializer,
                               final InferenceDeserializer deserializer,
                               final Region region,
                               final String accountId,
                               final ThumbnailS3PresignerFactory presignerFactory,
                               final ImageUploader imageUploader
                               ) {

        this.openSearchClientProvider = openSearchClientProvider;
        this.serializer = serializer;
        this.deserializer = deserializer;
        this.region = region;
        this.presignerFactory = presignerFactory;
        this.imageUploader = imageUploader;
        this.accountId = accountId;
    }

    public StreamsEventResponse handleRequest(final KinesisEvent event, final Context context) {
        LambdaLogger logger = context.getLogger();

        if (event == null) {
            throw new IllegalArgumentException(INVALID_INPUT_EXCEPTION);
        }
        OpenSearchClient openSearchClient = openSearchClientProvider.getInstance(this.endpoint);

        InferenceRequest inferenceRequest = parseKinesisEvent(event, logger);
        // https://docs.aws.amazon.com/lambda/latest/dg/with-kinesis.html#services-kinesis-batchfailurereporting
        List<BatchItemFailure> itemFailures = Lists.newArrayList();
        StringBuilder errorMessageBuilder = new StringBuilder();

        // Construct partial failure info
        populateInvalidInference(inferenceRequest, itemFailures, errorMessageBuilder);

        // Call Open Search API if there are any valid records from KDS
        if (!inferenceRequest.getValidRecords().isEmpty()) {
            // handle thumbnails
            for (Thumbnail thumbnail: inferenceRequest.getThumbnailUploadRequests()) {
                try {
                    thumbnail.upload();
                } catch (IOException e) {
                    logger.log(String.format("Failed to upload thumbnail to path: %s due to %s. Adding to list of failures to be retried in next lambda invocation.",
                            thumbnail.getS3UploadPath(), e.getMessage()));
                    itemFailures.add(BatchItemFailure.builder()
                            .withItemIdentifier(thumbnail.getSeqNumberInBatch())
                            .build());
                }
            }

            // Bulk index opensearch
            try {
                BulkResponse response = openSearchClient.bulkIndex(inferenceRequest.getBulkRequest());
                // Construct partial failure info
                populateOpenSearchPartialFailures(response, inferenceRequest, itemFailures, errorMessageBuilder, logger);
            } catch (IOException e) {
                throw new RuntimeException("bulkIndex API failed, sample partition key: "
                    + inferenceRequest.getValidRecords().get(0).getKinesis().getPartitionKey(), e);
            }
        }

        if (itemFailures.isEmpty()) {
            logger.log(String.format("Succeeded to index %d inferences from %d KDS records.",
                inferenceRequest.getValidRecords().size(), inferenceRequest.getAllRecords().size()));

            return null;
        }

        logger.log(
            String.format("Totally %d records from %d KDS records were not processed, with KDS sequences: %s. Error details: %s.",
            itemFailures.size(), inferenceRequest.getAllRecords().size(), itemFailures.toString(), errorMessageBuilder.toString()));

        return StreamsEventResponse.builder()
            .withBatchItemFailures(itemFailures)
            .build();
    }

    private void populateInvalidInference(
        final InferenceRequest inferenceRequest,
        final List<BatchItemFailure> itemFailures,
        final StringBuilder messageBuilder) {

        Integer indexOfInvalidRecord = inferenceRequest.getStartOfInvalidRecords();

        if (indexOfInvalidRecord != null) {
            for (int i = indexOfInvalidRecord; i < inferenceRequest.getAllRecords().size(); i++) {
                itemFailures.add(BatchItemFailure.builder()
                    .withItemIdentifier(inferenceRequest.getAllRecords().get(i).getKinesis().getSequenceNumber())
                    .build()
                );
            }

            messageBuilder.append(
                "Invalid partition key: " + inferenceRequest.getAllRecords().get(indexOfInvalidRecord)
                    .getKinesis().getPartitionKey());
        }
    }

    private void populateOpenSearchPartialFailures(
        final BulkResponse response,
        final InferenceRequest inferenceRequest,
        final List<BatchItemFailure> itemFailures,
        final StringBuilder messageBuilder,
        final LambdaLogger logger) {

        // Figure out each failed item's seqN fo partial failure handling
        if (response.hasFailures()) {
            List<String> itemFailureMessages = Lists.newArrayList();
            for (BulkItemResponse item : response.getItems()) {
                if (item.isFailed()) {
                    // Check if the failure is due to document with the same id existed
                    if (RestStatus.CONFLICT.equals(item.getFailure().getStatus())){
                        logger.log(String.format("Inference %s already exists, skip it.", item.getId()));
                        continue;
                    }
                    // Get original KDS seq number for the failed Open Search request
                    KinesisEvent.Record record = inferenceRequest.getValidRecords().get(item.getItemId()).getKinesis();
                    String seqNumber = record.getSequenceNumber();
                    itemFailures.add(BatchItemFailure.builder()
                        .withItemIdentifier(seqNumber)
                        .build()
                    );

                    itemFailureMessages.add(String.format("[partitionKey=%s, id=%s, message=%s]",
                        record.getPartitionKey(), item.getId(), item.getFailureMessage()));
                }
            }

            messageBuilder.append(
                String.format("Open Search failures (%d): %s.", itemFailureMessages.size(),
                    Joiner.on(" | ").join(itemFailureMessages)));
        }
    }

    private InferenceRequest parseKinesisEvent(KinesisEvent event, LambdaLogger logger) {
        List<KinesisEvent.KinesisEventRecord> kinesisEventRecords = event.getRecords();
        logger.log(String.format("Received %d inferences from KDS.", kinesisEventRecords.size()));
        BulkRequest bulkOpenSearchIndexRequest = new BulkRequest();
        List<KinesisEventRecord> validRecords = Lists.newArrayList();
        List<Thumbnail> thumbnailUploadRequests = Lists.newArrayList();

        for (int i = 0; i < kinesisEventRecords.size(); i++) {
            KinesisEvent.Record record = kinesisEventRecords.get(i).getKinesis();
            String kdsInferenceJson = new String(record.getData().array(), StandardCharsets.UTF_8);

            try {
                KdsInference kdsInference = deserializer.deserialize(kdsInferenceJson);
                OpenSearchInference inference = InferenceDeserializer.toOpenSearchInference(kdsInference);
                KdsMetadata kdsMetadata = kdsInference.getMetadata();
                // ThumbnailMetadata used for checksum and contentLength, needs to be incremented for each thumbnail.
                List<ThumbnailMetadata> thumbnailMetadataList = kdsMetadata.getThumbnailMetadata();

                if (kdsInference.getThumbnailS3Paths() == null || !inference.getThumbnailPayloads().isEmpty()) {
                    ArrayList<byte[]> thumbnailPayloads = inference.getThumbnailPayloads();
                    for (int index = 0; index < thumbnailPayloads.size(); index++) {
                        byte[] thumbnailPayload = thumbnailPayloads.get(index);
                        ThumbnailMetadata thumbnailMetadata = null;
                        if (!thumbnailMetadataList.isEmpty()) {
                            thumbnailMetadata = thumbnailMetadataList.get(index);
                        }
                        Thumbnail thumbnail = Thumbnail.builder()
                                .payload(thumbnailPayload)
                                .payloadSizeInBytes((long) thumbnailPayload.length)
                                .s3PresignerFactory(presignerFactory)
                                .imageUploader(imageUploader)
                                .seqNumberInBatch(record.getSequenceNumber())
                                .modelName(kdsMetadata.getModelName())
                                .modelVersion(kdsMetadata.getModelVersion())
                                .deviceId(kdsMetadata.getDeviceId())
                                .bucketName(String.format(UPLOAD_BUCKET_FORMAT, this.accountId, region.toString()))
                                .eventTimestamp(new DateTime(Long.parseLong(inference.getTimestamp())))
                                .eventDigest(kdsInference.getEventDigest())
                                .thumbnailMetadata(thumbnailMetadata)
                                .build();
                        // inject the S3 path to thumbnails into OpenSearch document
                        inference.addThumbnailS3Path(thumbnail.getS3UploadPath());
                        thumbnailUploadRequests.add(thumbnail);
                    }
                } else {
                    for (String thumbnailS3Path : kdsInference.getThumbnailS3Paths()) {
                        inference.addThumbnailS3Path(thumbnailS3Path);
                    }
                }
                String inferenceJson = serializer.serialize(inference);
                String dataStreamName = InferenceUtils.getOpenSearchDataStream(
                    kdsInference.getMetadata().getModelName(), kdsInference.getMetadata().getModelVersion());

                IndexRequest indexRequest = new IndexRequest(dataStreamName);
                indexRequest
                    .id(kdsInference.getOpenSearchDocumentId())
                    .source(inferenceJson, XContentType.JSON)
                    .opType(DocWriteRequest.OpType.CREATE);

                bulkOpenSearchIndexRequest.add(indexRequest);
                // Keep the record to retrieve error handling info in case Open Search API fails later
                validRecords.add(kinesisEventRecords.get(i));
            } catch (Exception e) {
                // Why don't we continue? Because the partial failure handling of Lambda will retry all records
                // starting from the invalid records. To avoid unnecessary call to Open Search at our best,
                // skip the processing of onward records
                logger.log(String.format("Failed to parse inference for partition %s with SeqN %s: %s",
                    record.getPartitionKey(), record.getSequenceNumber(), e.getMessage()));
                return new InferenceRequest(bulkOpenSearchIndexRequest, kinesisEventRecords, validRecords, i, thumbnailUploadRequests);
            }
        }

        return new InferenceRequest(bulkOpenSearchIndexRequest, kinesisEventRecords, validRecords, null, thumbnailUploadRequests);
    }
}
