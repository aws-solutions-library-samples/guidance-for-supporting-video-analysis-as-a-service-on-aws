package com.amazonaws.videoanalytics.videologistics.exceptions;

public class ResourceNotFoundException extends RuntimeException {
    private final String message;

    public ResourceNotFoundException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
