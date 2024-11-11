package com.amazonaws.videoanalytics.videologistics.exceptions;

public class AccessDeniedException extends RuntimeException {
    private final String message;

    public AccessDeniedException(String message) {
        super(message);
        this.message = message;
    }

    public AccessDeniedException(String message, Exception e) {
        super(message, e);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
