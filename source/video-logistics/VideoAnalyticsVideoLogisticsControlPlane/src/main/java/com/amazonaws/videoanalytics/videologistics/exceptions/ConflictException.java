package com.amazonaws.videoanalytics.videologistics.exceptions;

public class ConflictException extends RuntimeException {
    private final String message;

    public ConflictException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
