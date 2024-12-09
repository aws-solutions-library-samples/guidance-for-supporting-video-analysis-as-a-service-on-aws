package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.services.lambda.runtime.events.KinesisEvent.KinesisEventRecord;
import org.opensearch.action.bulk.BulkRequest;

import java.util.List;

public class InferenceRequest {
    private final BulkRequest bulkRequest;
    private final List<KinesisEventRecord> allRecords;
    private final List<KinesisEventRecord> validRecords;
    private final Integer startOfInvalidRecords;

    private final List<Thumbnail> thumbnailUploadRequests;

    public InferenceRequest(final BulkRequest bulkRequest, final List<KinesisEventRecord> allRecords,
                            final List<KinesisEventRecord> validRecords, final Integer startOfInvalidRecords,
                            final List<Thumbnail> thumbnailUploadRequests) {

        this.bulkRequest = bulkRequest;
        this.allRecords = allRecords;
        this.validRecords = validRecords;
        this.startOfInvalidRecords = startOfInvalidRecords;
        this.thumbnailUploadRequests = thumbnailUploadRequests;
    }

    public BulkRequest getBulkRequest() {
        return this.bulkRequest;
    }

    public List<KinesisEventRecord> getAllRecords() {
        return this.allRecords;
    }

    public List<KinesisEventRecord> getValidRecords() {
        return this.validRecords;
    }

    public Integer getStartOfInvalidRecords() {
        return this.startOfInvalidRecords;
    }

    public List<Thumbnail> getThumbnailUploadRequests() {
        return this.thumbnailUploadRequests;
    }
}