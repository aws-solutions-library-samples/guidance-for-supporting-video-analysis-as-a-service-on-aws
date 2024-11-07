package com.amazonaws.videoanalytics.exceptions;

import com.amazonaws.videoanalytics.videologistics.ValidationExceptionField;
import com.amazonaws.videoanalytics.videologistics.ValidationExceptionReason;

import java.util.List;

public class ValidationException extends RuntimeException {
    private final String message;
    private ValidationExceptionReason reason;
    private List<ValidationExceptionField> fieldList;

    public ValidationException(String message, ValidationExceptionReason reason, List<ValidationExceptionField> fieldList) {
        super(message);
        this.message = message;
        this.reason = reason;
        this.fieldList = fieldList;
    }

    public ValidationException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public ValidationExceptionReason getReason() {
        return reason;
    }

    public List<ValidationExceptionField> getFieldList() {
        return fieldList;
    }
}
