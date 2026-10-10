package com.mbanni.shop.checkout.dto;

import com.mbanni.shop.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record CheckoutReviewDto(
        Long orderId, Long userId, OrderStatus status, String stripeSessionId,
        BigDecimal total, Instant expiresAt, Instant reviewNeededAt, String reviewReason,
        Instant reviewResolvedAt, Long reviewedBy, String reviewResolution
) {}
