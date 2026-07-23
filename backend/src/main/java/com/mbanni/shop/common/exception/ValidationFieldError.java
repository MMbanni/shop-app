package com.mbanni.shop.common.exception;

public record ValidationFieldError(
        String field,
        String code,
        String message
) {
}