package com.amazonaws.videoanalytics.videologistics.inference;

import com.amazonaws.videoanalytics.videologistics.client.s3.ImageUploader;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3Presigner;
import com.amazonaws.videoanalytics.videologistics.client.s3.ThumbnailS3PresignerFactory;
import lombok.Builder;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.Base64;
import java.net.URL;

@Builder
public class Thumbnail {
    private final byte[] payload;
    private final Long payloadSizeInBytes;
    private final String modelName;
    private final String modelVersion;
    private final DateTime eventTimestamp;
    private final String eventDigest;
    private final String bucketName;
    private final String deviceId;
    private ThumbnailS3Presigner s3Presigner;
    private final ThumbnailS3PresignerFactory s3PresignerFactory;
    private final ImageUploader imageUploader;
    private final String seqNumberInBatch;
    private final ThumbnailMetadata thumbnailMetadata;

    private ThumbnailS3Presigner getOrCreateS3Presigner() {
        if (this.s3Presigner == null) {
            this.s3Presigner = s3PresignerFactory.create(
                    this.bucketName,
                    this.deviceId,
                    this.modelName,
                    this.modelVersion,
                    this.eventTimestamp,
                    this.eventDigest,
                    this.payloadSizeInBytes
            );
        }
        return this.s3Presigner;
    }

    public String getS3UploadPath() {
        return getOrCreateS3Presigner().getUploadPath();
    }

    public String getSeqNumberInBatch() {
        return this.seqNumberInBatch;
    }

    public void upload() throws IOException {
        imageUploader.upload(getOrCreateS3Presigner().generateImageUploadURL(payload), payload);
    }

    @Override
    public String toString() {
        return String.format("modelName=%s, modelVersion=%s, deviceId=%s, imageData=%s",
                modelName, modelVersion, deviceId, Base64.getEncoder().encodeToString(payload));
    }
    public URL generateImageUploadUrl(String checksum) {
        return getOrCreateS3Presigner().generateImageUploadURL(checksum);
    }
    public String getDeviceId() {
        return this.deviceId;
    }
    public String getChecksum() {
        return this.thumbnailMetadata.getChecksum();
    }
    public Long getContentLength() {
        return this.thumbnailMetadata.getContentLength();
    }
}