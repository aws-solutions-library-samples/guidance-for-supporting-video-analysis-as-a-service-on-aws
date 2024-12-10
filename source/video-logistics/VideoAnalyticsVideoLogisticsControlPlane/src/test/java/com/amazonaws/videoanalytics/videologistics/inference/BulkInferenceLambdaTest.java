package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClient;
import com.amazonaws.videoanalytics.videologistics.client.opensearch.OpenSearchClientProvider;
import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3Presigner;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import com.amazonaws.services.lambda.runtime.events.KinesisEvent.Record;
import com.amazonaws.services.lambda.runtime.events.StreamsEventResponse;
import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensearch.action.DocWriteRequest;
import org.opensearch.action.DocWriteResponse;
import org.opensearch.action.bulk.BulkItemResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.core.rest.RestStatus;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.IMAGE;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.KDS_INFERENCE_1;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.KDS_INFERENCE_2;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.KDS_INFERENCE_3;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.KDS_INFERENCE_4;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.KDS_INFERENCE_w_THUMBNAILS;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.OPEN_SEARCH_INFERENCE_JSON_1;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.OPEN_SEARCH_INFERENCE_JSON_2;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.OPEN_SEARCH_INFERENCE_JSON_3;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.OPEN_SEARCH_INFERENCE_JSON_4;
import static com.amazonaws.videoanalytics.videologistics.utils.InferenceTestUtils.THUMBNAIL_UPLOAD_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import software.amazon.awssdk.regions.Region;

public class BulkInferenceLambdaTest {
    private static final String MOCK_AWS_REGION = "mock-region-value";
    private static final String MOCK_AWS_STAGE = "Dev";
    private static final String MOCK_ACCOUNT_ID = "12312313132";

    @Rule
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Captor
    private ArgumentCaptor<BulkRequest> bulkRequestArgumentCaptor;

    @Captor
    private ArgumentCaptor<OpenSearchInference> openSearchInferenceArgumentCaptor;

    private BulkInferenceLambda bulkInferenceLambda;

    @Mock
    private OpenSearchClientProvider openSearchClientProvider;

    @Mock
    private OpenSearchClient openSearchClient;

    @Mock
    private InferenceSerializer serializer;

    @Mock
    private InferenceDeserializer deserializer;

    @Mock
    private Context context;

    @Mock
    private BulkResponse bulkResponse;

    @Mock
    private ThumbnailS3PresignerFactory s3PresignerFactory;

    @Mock
    private ImageUploader imageUploader;

    @Mock
    private ThumbnailS3Presigner s3Presigner;

    @Mock
    private AwsCredentialsProvider fasCreds;

    @BeforeEach
    public void setup() throws MalformedURLException {
        environmentVariables.set("AWS_REGION", MOCK_AWS_REGION);
        environmentVariables.set("Stage", MOCK_AWS_STAGE);
        MockitoAnnotations.openMocks(this);
        when(openSearchClientProvider.getInstance(any(String.class))).thenReturn(openSearchClient);
        when(s3Presigner.getUploadPath()).thenReturn(THUMBNAIL_UPLOAD_PATH);
        when(s3Presigner.generateImageUploadURL(any(byte[].class))).thenReturn(new URL("https://" + THUMBNAIL_UPLOAD_PATH));
        when(s3Presigner.generateImageUploadURL(any(String.class))).thenReturn(new URL("https://" + THUMBNAIL_UPLOAD_PATH));
        when(s3PresignerFactory.create(any(), any(), any(),
                any(), any(), any(), any())).thenReturn(s3Presigner);
        bulkInferenceLambda = new BulkInferenceLambda(openSearchClientProvider, serializer, deserializer, Region.of(MOCK_AWS_REGION), MOCK_ACCOUNT_ID, s3PresignerFactory, imageUploader);
    }

    @Test
    public void bulkInferenceHappyPathTest() throws IOException {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_1, KDS_INFERENCE_2));

        when(deserializer.deserialize(anyString()))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_1))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_2));

        when(serializer.serialize(openSearchInferenceArgumentCaptor.capture()))
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_1)
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_2);

        when(openSearchClient.bulkIndex(bulkRequestArgumentCaptor.capture()))
            .thenReturn(bulkResponse);
        when(bulkResponse.hasFailures()).thenReturn(false);

        StreamsEventResponse response = bulkInferenceLambda.handleRequest(event, context);

        verify(openSearchClient, times(1)).bulkIndex(bulkRequestArgumentCaptor.capture());
        assertEquals(2, bulkRequestArgumentCaptor.getValue().requests().size());
        assertNull(response);

        IndexRequest indexRequest1 = (IndexRequest) bulkRequestArgumentCaptor.getValue().requests().get(0);
        IndexRequest indexRequest2 = (IndexRequest) bulkRequestArgumentCaptor.getValue().requests().get(1);

        assertEquals("test-1.0", indexRequest1.index());
        assertEquals(XContentType.JSON, indexRequest1.getContentType());
        assertEquals(OPEN_SEARCH_INFERENCE_JSON_1, indexRequest1.source().utf8ToString());

        String expectedIndex = "test-1.0";
        String expectedId = "Device#456-2023-10-07T00:41:47.418Z-Test-1.0-44288fd324546f02fe39f5d4e5961e9b260c2e51ffb11f704b6c430a878d03f78054e65c517cef394c2b19d713bf5d1a";
        assertEquals(expectedIndex, indexRequest2.index());
        assertEquals(expectedId, indexRequest2.id());
        assertEquals(XContentType.JSON, indexRequest2.getContentType());
        assertEquals(OPEN_SEARCH_INFERENCE_JSON_2, indexRequest2.source().utf8ToString());

        // verify that the image was uploaded AND path to the uploaded image was serialized to OpenSeach json
        verify(imageUploader, times(3)).upload(new URL("https://" + THUMBNAIL_UPLOAD_PATH), IMAGE.getBytes(Charset.defaultCharset()));
        assertEquals(THUMBNAIL_UPLOAD_PATH, openSearchInferenceArgumentCaptor.getValue().getMetadata().getThumbnailS3Paths().get(0));
        assertEquals(THUMBNAIL_UPLOAD_PATH, openSearchInferenceArgumentCaptor.getValue().getMetadata().getThumbnailS3Paths().get(1));
    }

    @Test
    public void bulkInferenceHappyPathTestWithThumbnails() throws IOException {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_w_THUMBNAILS, KDS_INFERENCE_2));

        when(deserializer.deserialize(anyString()))
                .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_w_THUMBNAILS))
                .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_2));

        when(serializer.serialize(openSearchInferenceArgumentCaptor.capture()))
                .thenReturn(OPEN_SEARCH_INFERENCE_JSON_1)
                .thenReturn(OPEN_SEARCH_INFERENCE_JSON_2);

        when(openSearchClient.bulkIndex(bulkRequestArgumentCaptor.capture()))
                .thenReturn(bulkResponse);
        when(bulkResponse.hasFailures()).thenReturn(false);

        StreamsEventResponse response = bulkInferenceLambda.handleRequest(event, context);

        verify(openSearchClient, times(1)).bulkIndex(bulkRequestArgumentCaptor.capture());
        assertEquals(2, bulkRequestArgumentCaptor.getValue().requests().size());
        assertNull(response);

        IndexRequest indexRequest1 = (IndexRequest) bulkRequestArgumentCaptor.getValue().requests().get(0);
        IndexRequest indexRequest2 = (IndexRequest) bulkRequestArgumentCaptor.getValue().requests().get(1);

        assertEquals("test-1.0", indexRequest1.index());
        assertEquals(XContentType.JSON, indexRequest1.getContentType());
        assertEquals(OPEN_SEARCH_INFERENCE_JSON_1, indexRequest1.source().utf8ToString());

        String expectedIndex = "test-1.0";
        String expectedId = "Device#456-2023-10-07T00:41:47.418Z-Test-1.0-44288fd324546f02fe39f5d4e5961e9b260c2e51ffb11f704b6c430a878d03f78054e65c517cef394c2b19d713bf5d1a";
        assertEquals(expectedIndex, indexRequest2.index());
        assertEquals(expectedId, indexRequest2.id());
        assertEquals(XContentType.JSON, indexRequest2.getContentType());
        assertEquals(OPEN_SEARCH_INFERENCE_JSON_2, indexRequest2.source().utf8ToString());

        // verify that the image was uploaded AND path to the uploaded image was serialized to OpenSeach json
        verify(imageUploader, times(2)).upload(new URL("https://" + THUMBNAIL_UPLOAD_PATH), IMAGE.getBytes(Charset.defaultCharset()));
        assertEquals(THUMBNAIL_UPLOAD_PATH, openSearchInferenceArgumentCaptor.getValue().getMetadata().getThumbnailS3Paths().get(0));
        assertEquals(THUMBNAIL_UPLOAD_PATH, openSearchInferenceArgumentCaptor.getValue().getMetadata().getThumbnailS3Paths().get(1));
    }

    @Test
    public void bulkInferenceTest_nullInput() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bulkInferenceLambda.handleRequest(null, context);
        });

        assertEquals("Malformed input, please fix the input.", exception.getMessage());
    }

    @Test
    public void bulkInferenceTest_bulkIndexException() throws IOException {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_1, KDS_INFERENCE_2));

        when(deserializer.deserialize(anyString()))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_1))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_2));

        when(serializer.serialize(openSearchInferenceArgumentCaptor.capture()))
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_1)
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_2);

        when(openSearchClient.bulkIndex(bulkRequestArgumentCaptor.capture())).thenThrow(IOException.class);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bulkInferenceLambda.handleRequest(event, context);
        });

        assertEquals("bulkIndex API failed, sample partition key: Dummy Key", exception.getMessage());
    }

    @Test
    public void bulkInferenceTest_imageUploadPartialFailure() throws IOException {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_1, KDS_INFERENCE_2));
        when(deserializer.deserialize(anyString()))
                .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_1))
                .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_3));
        when(serializer.serialize(openSearchInferenceArgumentCaptor.capture()))
                .thenReturn(OPEN_SEARCH_INFERENCE_JSON_1)
                .thenReturn(OPEN_SEARCH_INFERENCE_JSON_3);
        when(openSearchClient.bulkIndex(any()))
                .thenReturn(bulkResponse);
        when(bulkResponse.hasFailures()).thenReturn(false);

        // Throw exception on the second image upload
        doNothing().doThrow(IOException.class)
                .when(imageUploader).upload(any(), any());

        StreamsEventResponse actualResponse = bulkInferenceLambda.handleRequest(event, context);

        List<StreamsEventResponse.BatchItemFailure> failures = Lists.newArrayList(
                StreamsEventResponse.BatchItemFailure.builder().withItemIdentifier("2").build()
        );
        StreamsEventResponse expectedResponse = StreamsEventResponse.builder()
                .withBatchItemFailures(failures)
                .build();

        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void bulkInferenceTest_bulkAPIFailure() throws Exception {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_1, KDS_INFERENCE_2, KDS_INFERENCE_3, KDS_INFERENCE_4));

        when(deserializer.deserialize(anyString()))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_1))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_2))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_3))
            .thenReturn(InferenceTestUtils.getKdsInference(KDS_INFERENCE_4));

        when(serializer.serialize(openSearchInferenceArgumentCaptor.capture()))
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_1)
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_2)
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_3)
            .thenReturn(OPEN_SEARCH_INFERENCE_JSON_4);

        BulkItemResponse response1 = new BulkItemResponse(0, DocWriteRequest.OpType.CREATE, (DocWriteResponse) null);
        BulkItemResponse response2 = new BulkItemResponse(1, DocWriteRequest.OpType.CREATE,
            new BulkItemResponse.Failure("dummyIndex", "dummyId", new RuntimeException()));
        BulkItemResponse response3 = new BulkItemResponse(2, DocWriteRequest.OpType.CREATE,
            // Simulate document existing scenario
            new BulkItemResponse.Failure("dummyIndex", "dummyId", new RuntimeException(), RestStatus.CONFLICT));
        BulkItemResponse response4 = new BulkItemResponse(3, DocWriteRequest.OpType.CREATE, (DocWriteResponse) null);

        when(openSearchClient.bulkIndex(bulkRequestArgumentCaptor.capture())).thenReturn(bulkResponse);
        when(bulkResponse.hasFailures()).thenReturn(true);
        when(bulkResponse.getItems()).thenReturn(new BulkItemResponse[] {response1, response2, response3, response4});

        StreamsEventResponse actualResponse = bulkInferenceLambda.handleRequest(event, context);

        List<StreamsEventResponse.BatchItemFailure> failures = Lists.newArrayList(
            StreamsEventResponse.BatchItemFailure.builder().withItemIdentifier("2").build()
        );

        StreamsEventResponse expectedResponse = StreamsEventResponse.builder()
            .withBatchItemFailures(failures)
            .build();

        verify(openSearchClient, times(1)).bulkIndex(bulkRequestArgumentCaptor.capture());
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    public void bulkInferenceHappyPathTest_InvalidInferenceFormat() throws IOException {
        KinesisEvent event = getKinesisEvent(Lists.newArrayList(KDS_INFERENCE_1, KDS_INFERENCE_2));

        when(deserializer.deserialize(anyString()))
            .thenThrow(new RuntimeException());

        StreamsEventResponse actualResponse = bulkInferenceLambda.handleRequest(event, context);
        List<StreamsEventResponse.BatchItemFailure> failures = Lists.newArrayList(
            StreamsEventResponse.BatchItemFailure
                .builder()
                .withItemIdentifier("1")
                .build(),
            StreamsEventResponse.BatchItemFailure
                .builder()
                .withItemIdentifier("2")
                .build());

        StreamsEventResponse expectedResponse = StreamsEventResponse.builder()
            .withBatchItemFailures(failures)
            .build();

        verify(openSearchClient, times(0)).bulkIndex(bulkRequestArgumentCaptor.capture());
        assertEquals(expectedResponse, actualResponse);
    }

    private KinesisEvent getKinesisEvent(List<String> inferenceJsonList) {
        List<KinesisEventRecord> kinesisEventRecords = new ArrayList<>();
        // Seq Number starts with 1
        int seqN = 1;
        for (String inferenceJson : inferenceJsonList) {
            final KinesisEventRecord kinesisEventRecord = new KinesisEventRecord();

            ByteBuffer byteBuffer = ByteBuffer.wrap(inferenceJson.getBytes(StandardCharsets.UTF_8));

            Record rec =  (Record) new Record()
                .withPartitionKey(InferenceTestUtils.DUMMY_PARTITION_KEY)
                .withSequenceNumber(String.valueOf(seqN++))
                .withData(byteBuffer)
                .withApproximateArrivalTimestamp(new Date());

            kinesisEventRecord.setKinesis(rec);
            kinesisEventRecords.add(kinesisEventRecord);
        }

        KinesisEvent kinesisEvent = new KinesisEvent();
        kinesisEvent.setRecords(kinesisEventRecords);

        return kinesisEvent;
    }
}