package com.mbanni.shop.common.exception;

public class RateLimiterUnavailableException
        extends RuntimeException {

    public RateLimiterUnavailableException(Throwable cause) {
        super("Login rate limiter is unavailable", cause);
    }
}
