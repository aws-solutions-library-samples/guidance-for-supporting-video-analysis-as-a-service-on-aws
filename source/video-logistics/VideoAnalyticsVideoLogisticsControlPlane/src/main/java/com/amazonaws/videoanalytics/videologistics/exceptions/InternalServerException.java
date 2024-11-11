package com.amazonaws.videoanalytics.videologistics.exceptions;

public class InternalServerException extends RuntimeException {
    private final String message;

    public InternalServerException(String message) {
        super(message);
        this.message = message;
    }

    public InternalServerException(String message, Exception e) {
        super(message, e);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
