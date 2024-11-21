package com.amazonaws.videoanalytics.videologistics.client.s3;

import java.util.Base64;
import org.apache.commons.codec.digest.DigestUtils;

public class S3ChecksumCalculator {

    private S3ChecksumCalculator(){}

    public static String checksum256(final byte[] payload) {
        return Base64.getEncoder().encodeToString(DigestUtils.sha256(payload));

    }

}