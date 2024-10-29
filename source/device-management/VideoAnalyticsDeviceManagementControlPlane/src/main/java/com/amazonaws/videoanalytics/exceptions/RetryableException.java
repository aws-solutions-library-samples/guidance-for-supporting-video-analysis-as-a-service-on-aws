package com.amazonaws.videoanalytics.exceptions;

public class RetryableException extends RuntimeException {
    public RetryableException(String errorMessage) {
        super(errorMessage);
    }
}
