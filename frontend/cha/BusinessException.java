package com.mbanni.shop.common.exception;


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

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
