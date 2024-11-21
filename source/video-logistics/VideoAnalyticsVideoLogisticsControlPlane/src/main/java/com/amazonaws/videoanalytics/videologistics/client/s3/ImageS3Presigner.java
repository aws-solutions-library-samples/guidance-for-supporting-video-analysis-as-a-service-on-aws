package com.amazonaws.videoanalytics.videologistics.client.s3;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

public abstract class ImageS3Presigner {
    private static final String IMAGE_MIME_TYPE = "image/jpeg";
    private static final Duration EXPIRATION_DURATION = Duration.ofMinutes(10);
    private static final String SNAPSHOT_S3_KEY_FORMAT = "snapshots/%s/snapshot.jpeg"; //snapshots/<deviceId>/snapshot.jpeg

    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String deviceId;
    private final Long sizeInBytes;

    public ImageS3Presigner(final S3Presigner s3Presigner,
                                  final String bucketName,
                                  final String deviceId,
                                  final Long sizeInBytes) {
        this.s3Presigner = s3Presigner;
        this.deviceId = deviceId;
        this.bucketName = bucketName;
        this.sizeInBytes = sizeInBytes;
    }

    public URL generateImageUploadURL(byte[] payload) {
        final PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(getUploadKey())
                .contentType(IMAGE_MIME_TYPE)
                .contentLength(sizeInBytes)
                .checksumSHA256(S3ChecksumCalculator.checksum256(payload))
                .build();

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(EXPIRATION_DURATION)
                .putObjectRequest(objectRequest)
                .build();
        final PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url();
    }

    public URL generateImageUploadURL(String checksum) {
        final PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(getUploadKey())
                .contentType(IMAGE_MIME_TYPE)
                .contentLength(sizeInBytes)
                .checksumSHA256(checksum)
                .build();

        final PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(EXPIRATION_DURATION)
                .putObjectRequest(objectRequest)
                .build();
        final PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url();
    }

    public abstract String getUploadKey();

    public String getUploadPath() {
        return String.format("s3://%s/%s", this.bucketName, getUploadKey());
    }

    protected String getDeviceId() {
        return this.deviceId;
    }
    
}
