package com.mbanni.shop.order.dto;

import com.mbanni.shop.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsDto(
        Long id,
        OrderStatus status,
        Instant createdAt,
        Instant paidAt,
        BigDecimal total,
        List<OrderItemDto> items
) {
}
