package com.amazonaws.videoanalytics.videologistics.client.s3;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class SnapshotS3Presigner extends ImageS3Presigner {
    private static final String SNAPSHOT_S3_KEY_FORMAT = "snapshots/%s/snapshot.jpeg"; //snapshots/<deviceId>/snapshot.jpeg

    public SnapshotS3Presigner(final S3Presigner s3Presigner,
                               final String bucketName,
                               final String deviceId,
                               final Long sizeInBytes) {
        super(s3Presigner, bucketName, deviceId, sizeInBytes);
    }

    @Override
    public String getUploadKey() {
        return String.format(SNAPSHOT_S3_KEY_FORMAT, getDeviceId());
    }
}
