package com.mbanni.shop.common.exception;

import java.util.List;
import java.util.Map;

public class BusinessException extends RuntimeException{

    private final ErrorCode errorCode;
    private final Map<String, Object> properties;

    // Allow for custom message if needed
    public BusinessException( ErrorCode errorCode, String message, Map<String, Object> properties) {
        super(message);
        this.errorCode = errorCode;
        this.properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, Map.of());
    }

    public BusinessException( ErrorCode errorCode, Map<String, Object> properties) {
        this(errorCode, errorCode.getDefaultMessage(), properties);
    }

    public BusinessException( ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage(), Map.of());
    }

    public static BusinessException forField(
            ErrorCode errorCode,
            String field,
            String message
    ) {
        ValidationFieldError fieldError = new ValidationFieldError(
                field,
                errorCode.name(),
                message
        );

        return new BusinessException(
                errorCode,
                message,
                Map.of("errors", List.of(fieldError))
        );
    }

    public static BusinessException forField(
            ErrorCode errorCode,
            String field
    ) {
        return forField(errorCode, field, errorCode.getDefaultMessage());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
