package com.mbanni.shop.order.dto;

import com.mbanni.shop.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderSnapshot(
        Long orderId, Long userId, OrderStatus status,
        String stripeSessionId, String checkoutUrl,
        Instant createdAt, Instant expiresAt, Instant paidAt,
        BigDecimal total, String successUrl, String cancelUrl,
        Instant reviewNeededAt, List<Line> items
) {
    public OrderSnapshot {
        items = List.copyOf(items);
    }

    public boolean isPending() { return status == OrderStatus.PENDING; }
    public boolean needsReview() { return reviewNeededAt != null; }
    public boolean hasCreationSettings() { return successUrl != null && cancelUrl != null; }

    public OrderResponseDto toResponse() {
        return new OrderResponseDto(orderId, status, paidAt);
    }

    public record Line(Long id, String productName, int quantity, BigDecimal unitPrice) {}
}
