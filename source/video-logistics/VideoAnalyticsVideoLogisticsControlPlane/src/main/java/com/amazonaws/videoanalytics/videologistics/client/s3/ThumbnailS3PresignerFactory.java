package com.amazonaws.videoanalytics.videologistics.client.s3;

import org.joda.time.DateTime;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class ThumbnailS3PresignerFactory {
    public ThumbnailS3Presigner create(final String bucketName,
                                       final String deviceId,
                                       final String modelName,
                                       final String modelVersion,
                                       final DateTime eventTimestamp,
                                       final String eventDigest,
                                       final Long payloadSizeInBytes) {
        return new ThumbnailS3Presigner(
                S3Presigner.builder()
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .build(),
                bucketName,
                deviceId,
                modelName,
                modelVersion,
                eventTimestamp,
                eventDigest,
                payloadSizeInBytes
        );
    }

}