package com.amazonaws.videoanalytics.devicemanagement.exceptions;

public class RetryableException extends RuntimeException {
    public RetryableException(String errorMessage) {
        super(errorMessage);
    }
}
