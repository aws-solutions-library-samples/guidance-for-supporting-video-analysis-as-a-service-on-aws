package com.amazonaws.videoanalytics.videologistics.utils;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.regions.Region;
import static org.junit.Assert.assertEquals;

public class S3BucketRegionalizerTest {
    
    private S3BucketRegionalizer regionalizer;
    private static final String SERVICE_ACCOUNT_ID = "1234567890";
    private static final Region REGION = Region.US_EAST_1;
    
    @Before
    public void setup() {
        regionalizer = new S3BucketRegionalizer(REGION, SERVICE_ACCOUNT_ID);
    }
    
    @Test
    public void getRegionalizedBucketName_WithValidInput() {
        String bucketName = "my-bucket";
        String expected = "my-bucket-us-east-1-1234567890";
        assertEquals(expected, regionalizer.getRegionalizedBucketName(bucketName));
    }
}