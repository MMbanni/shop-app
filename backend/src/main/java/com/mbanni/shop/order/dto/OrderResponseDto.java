package com.mbanni.shop.order.dto;

import com.mbanni.shop.order.OrderStatus;

import java.time.Instant;

public record OrderResponseDto(
        Long orderId,
        OrderStatus status,
        Instant paidAt
) {
}