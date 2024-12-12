package com.amazonaws.videoanalytics.videologistics.client.s3;

import javax.inject.Inject;
import javax.inject.Singleton;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;

import static com.amazonaws.videoanalytics.videologistics.exceptions.VideoAnalyticsExceptionMessage.S3_BUCKET_CREATION_ERROR;

@Singleton
public class S3Proxy {
    private static final Logger LOG = LoggerFactory.getLogger(S3Proxy.class);

    public static final String FWD_RULES_ACCESS_LOG_BUCKET_PREFIX = "%s/logs";

    private final S3Client s3Client;
    
    @Inject
    public S3Proxy(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /** BUCKET CREATION AND POLICIES */

    public boolean doesBucketExist(final String bucketName) {
        LOG.info("Checking if S3 bucket exist: [{}]", bucketName);
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
        } catch (final NoSuchBucketException e) {
            LOG.info("[{}] does not exist", bucketName);
            return false;
        }
        LOG.info("[{}] exists", bucketName);
        return true;
    }

    public void createBucket(final String bucketName, final String accessLogsBucketName) {
        LOG.info("Creating S3 bucket: [{}]", bucketName);

        s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        try {
            enableServerAccessLogging(bucketName, accessLogsBucketName,
                    String.format(FWD_RULES_ACCESS_LOG_BUCKET_PREFIX, bucketName));
            enableVersioningForNewBucket(bucketName);
            enableEncryption(bucketName);
            blockPublicAccess(bucketName);
            deniesNonSecureTransport(bucketName);
        } catch(S3Exception e) {
            LOG.info("Deleting newly created bucket as bucket configurations could not be set successfully");
            deleteBucket(bucketName);
            throw new RuntimeException(S3_BUCKET_CREATION_ERROR);
        }

        LOG.info("S3 bucket [{}] created successfully", bucketName);
    }

    private void enableServerAccessLogging(String sourceBucketName, String targetBucketName,
                                         String targetPrefix) {
        LOG.info("Enabling server access logging for {}, target bucket {}", sourceBucketName, targetBucketName);
        
        LoggingEnabled loggingEnabled = LoggingEnabled.builder()
                .targetBucket(targetBucketName)
                .targetPrefix(targetPrefix)
                .build();
                
        BucketLoggingStatus bucketLoggingStatus = BucketLoggingStatus.builder()
                .loggingEnabled(loggingEnabled)
                .build();
                
        s3Client.putBucketLogging(PutBucketLoggingRequest.builder()
                .bucket(sourceBucketName)
                .bucketLoggingStatus(bucketLoggingStatus)
                .build());
    }

    // Enable versioning for newly created bucket
    private void enableVersioningForNewBucket(String bucketName) {
        LOG.info("Enabling versioning for {}", bucketName);
        PutBucketVersioningRequest setBucketVersioningConfigurationRequest = PutBucketVersioningRequest.builder()
                .bucket(bucketName)
                .versioningConfiguration(VersioningConfiguration.builder().status("Enabled").build())
                .build();

        s3Client.putBucketVersioning(setBucketVersioningConfigurationRequest);
    }

    // https://docs.aws.amazon.com/AmazonS3/latest/userguide/specifying-s3-encryption.html
    private void enableEncryption(String bucketName) {
        LOG.info("Enabling encryption for {}", bucketName);
        PutBucketEncryptionRequest putBucketEncryptionRequest = PutBucketEncryptionRequest.builder()
                .bucket(bucketName)
                .serverSideEncryptionConfiguration(
                        ServerSideEncryptionConfiguration.builder().rules(
                                ServerSideEncryptionRule.builder().applyServerSideEncryptionByDefault(
                                        ServerSideEncryptionByDefault.builder().sseAlgorithm(
                                                ServerSideEncryption.AES256).build()
                                ).build()
                        ).build()
                ).build();
        this.s3Client.putBucketEncryption(putBucketEncryptionRequest);
    }

    // https://www.aristotle.a2z.com/implementations/8
    private void deniesNonSecureTransport(String bucketName) {
        LOG.info("Denying non secure transport for {}", bucketName);
        String policyStatement = String.format(
                "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Action\":\"s3:*\",\"Effect\":\"Deny\"," +
                        "\"Resource\":[\"arn:aws:s3:::%s\",\"arn:aws:s3:::%s/*\"]," +
                        "\"Condition\":{\"Bool\":{\"aws:SecureTransport\":\"false\"}},\"Principal\":\"*\"}]}",
                bucketName, bucketName);
        PutBucketPolicyRequest putBucketPolicyRequest = PutBucketPolicyRequest.builder()
                .bucket(bucketName)
                .policy(policyStatement).build();
        this.s3Client.putBucketPolicy(putBucketPolicyRequest);
    }

    // Block public access for newly created bucket
    private void blockPublicAccess(String bucketName) {
        LOG.info("Blocking public access for {}", bucketName);
        // https://docs.aws.amazon.com/AmazonS3/latest/userguide/configuring-block-public-access-bucket.html
        PutPublicAccessBlockRequest putPublicAccessBlockRequest = PutPublicAccessBlockRequest.builder()
                .bucket(bucketName)
                .publicAccessBlockConfiguration(PublicAccessBlockConfiguration.builder()
                        .blockPublicAcls(true)
                        .ignorePublicAcls(true)
                        .blockPublicPolicy(true)
                        .restrictPublicBuckets(true)
                        .build())
                .build();
        this.s3Client.putPublicAccessBlock(putPublicAccessBlockRequest);
    }

    /**
     * Util to delete an s3 bucket and throw an exception if the bucket does not exist
     *
     * @param bucketName the bucket name
     */
    public void deleteBucket(final String bucketName) {
        LOG.info("Deleting S3 bucket: [{}]", bucketName);
        s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
    }

    /** Object operations*/
    public void putObjectBytes(String bucketName, String key, byte[] requestBody) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(requestBody));
    }

    public ResponseBytes<GetObjectResponse> getS3ObjectBytes(String bucketName, String key) throws Exception {
        LOG.info(String.format("Retrieving data from s3://%s/%s", bucketName, key));
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObjectAsBytes(getObjectRequest);
    }

    public InputStream getObject(final String bucketName, final String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        return s3Client.getObject(getObjectRequest);
    }

    public ListObjectsV2Response listS3Objects(String bucketName, String continuationToken) {
        ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .continuationToken(continuationToken)
                .build();
        return s3Client.listObjectsV2(listObjectsRequest);
    }

    public void deleteObject(final String bucketName, final String keyPrefix) {
        LOG.info("Start to delete object from bucket {} and path {}", bucketName, keyPrefix);

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(keyPrefix)
                .build());
        LOG.info("Object deleted successfully for bucket {} and path {}", bucketName, keyPrefix);
    }
}