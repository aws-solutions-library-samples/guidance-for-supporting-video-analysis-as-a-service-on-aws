package com.amazonaws.videoanalytics.config;

/**
 * Example interface of a static configuration handler
 */
public interface Configuration {

    /**
     * Returns the AWS code of the AWS region where the application is running in
     * @return an AWS code (e.g. us-west-2)
     */
    String getRegion();
}
