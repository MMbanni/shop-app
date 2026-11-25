package com.mbanni.shop.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        Long productId,
        String name,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
