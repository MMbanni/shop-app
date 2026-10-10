package com.mbanni.shop.payment.dto;

import com.stripe.model.checkout.Session;

import java.util.Map;

public record StripeSessionSnapshot(
        String id, String url, String status, String paymentStatus,
        String mode, String currency, Long amountTotal,
        String clientReferenceId, Map<String, String> metadata
) {
    public StripeSessionSnapshot {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static StripeSessionSnapshot from(Session session) {
        return new StripeSessionSnapshot(
                session.getId(), session.getUrl(), session.getStatus(),
                session.getPaymentStatus(), session.getMode(), session.getCurrency(),
                session.getAmountTotal(), session.getClientReferenceId(), session.getMetadata()
        );
    }

    public boolean isOpen() { return "open".equals(status); }
    public boolean isComplete() { return "complete".equals(status); }
    public boolean isExpired() { return "expired".equals(status); }

    public boolean isPaid() {
        return isComplete() && ("paid".equals(paymentStatus)
                || ("no_payment_required".equals(paymentStatus) && Long.valueOf(0L).equals(amountTotal)));
    }
}
