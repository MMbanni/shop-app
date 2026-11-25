package com.mbanni.shop.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryDto(
        Long id,
        String status,
        Instant createdAt,
        BigDecimal total
) {}
