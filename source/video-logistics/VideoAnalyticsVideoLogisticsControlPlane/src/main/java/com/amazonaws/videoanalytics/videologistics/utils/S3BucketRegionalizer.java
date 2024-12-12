package com.amazonaws.videoanalytics.videologistics.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.regions.Region;

import javax.inject.Inject;

public class S3BucketRegionalizer {
    private static final Logger LOG = LogManager.getLogger(S3BucketRegionalizer.class);
    /**
     * Class to generate s3 bucket name. Adds service account id and region to bucket name.
     */
    private final Region region;
    private final String serviceAccountId;
    @Inject
    public S3BucketRegionalizer(final Region region, final String serviceAccountId) {
        this.region = region;
        this.serviceAccountId = serviceAccountId;
    }

    public String getRegionalizedBucketName(String bucketName) {
        LOG.info(String.format("Generating regionalized bucket name for bucket name %s, region %s and account id %s",
                bucketName, region.toString().toLowerCase(), serviceAccountId));
        return String.format(bucketName + "-%s-%s", region.toString().toLowerCase(), serviceAccountId);
    }
}
