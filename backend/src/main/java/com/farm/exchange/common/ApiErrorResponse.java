package com.farm.exchange.common;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiErrorResponse {

    private final boolean success;
    private final String code;
    private final int status;
    private final String message;
    private final String path;
    private final OffsetDateTime timestamp;
    private final List<FieldErrorItem> fieldErrors;

    public ApiErrorResponse(String code, int status, String message, String path, List<FieldErrorItem> fieldErrors) {
        this.success = false;
        this.code = code;
        this.status = status;
        this.message = message;
        this.path = path;
        this.timestamp = OffsetDateTime.now();
        this.fieldErrors = fieldErrors;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }

    public static class FieldErrorItem {
        private final String field;
        private final String message;

        public FieldErrorItem(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }
    }
}
