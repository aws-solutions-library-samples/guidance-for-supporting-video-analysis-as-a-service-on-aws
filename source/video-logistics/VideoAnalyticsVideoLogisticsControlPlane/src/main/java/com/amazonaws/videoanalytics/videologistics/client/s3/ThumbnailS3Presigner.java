package com.amazonaws.videoanalytics.videologistics.client.s3;

import org.joda.time.DateTime;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class ThumbnailS3Presigner extends ImageS3Presigner {

    // Breaking up events into separate prefixes to distribute load across S3 prefixes per
    // https://docs.aws.amazon.com/AmazonS3/latest/userguide/optimizing-performance.html
    // event-thumbnails/<device-id>/<modelName>/<modelVersion>/<event-year>/<event-month>/<event-day>/<event-hour>/
    // event-<timestamp>-<eventdigest>.jpeg
    private static final String EVENT_THUMBNAIL_S3_KEY_FORMAT = "event-thumbnails/%s/%s/%s/%s/%s/%s/%s/event-%s-%s.jpeg";

    private final String modelName;
    private final String modelVersion;
    private final DateTime eventTimestamp;
    private final String eventDigest;

    public ThumbnailS3Presigner(final S3Presigner s3Presigner,
                                final String bucketName,
                                final String deviceId,
                                final String modelName,
                                final String modelVersion,
                                final DateTime eventTimestamp,
                                final String eventDigest,
                                final Long sizeInBytes) {
        super(s3Presigner, bucketName, deviceId, sizeInBytes);
        this.modelName = modelName;
        this.modelVersion = modelVersion;
        this.eventTimestamp = eventTimestamp;
        this.eventDigest = eventDigest;
    }

    @Override
    public String getUploadKey() {
        return String.format(EVENT_THUMBNAIL_S3_KEY_FORMAT,
                getDeviceId(),
                modelName,
                modelVersion,
                eventTimestamp.getYear(),
                eventTimestamp.getMonthOfYear(),
                eventTimestamp.getDayOfMonth(),
                eventTimestamp.getHourOfDay(),
                eventTimestamp,
                eventDigest);
    }
}