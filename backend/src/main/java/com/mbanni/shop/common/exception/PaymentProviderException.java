package com.mbanni.shop.common.exception;

public class PaymentProviderException extends RuntimeException {

    private final boolean temporary;

    public PaymentProviderException(boolean temporary, Throwable cause) {
        super("Payment provider operation failed", cause);
        this.temporary = temporary;
    }

    public boolean isTemporary() {
        return temporary;
    }
}