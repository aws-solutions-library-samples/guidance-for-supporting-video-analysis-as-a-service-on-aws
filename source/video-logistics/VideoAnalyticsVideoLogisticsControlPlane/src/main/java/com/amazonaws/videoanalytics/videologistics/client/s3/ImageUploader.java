package com.amazonaws.videoanalytics.videologistics.client.s3;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageUploader {

    public void upload(final URL uploadPath, final byte[] payload) throws IOException {

        // Create the connection and use it to upload the new object by using the presigned URL.
        final HttpURLConnection connection = (HttpURLConnection) uploadPath.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type","image/jpeg");
        connection.setRequestProperty("x-amz-checksum-sha256", S3ChecksumCalculator.checksum256(payload));
        connection.setRequestMethod("PUT");
        connection.getOutputStream().write(payload);

        final int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException(String.format("Failed to upload to path %s due to ERROR CODE: %s, ERROR MESSAGE: %s",
                    uploadPath, connection.getResponseCode(), connection.getResponseMessage()));
        }
    }

}