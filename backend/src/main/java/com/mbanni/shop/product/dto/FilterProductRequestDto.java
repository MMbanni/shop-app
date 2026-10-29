package com.mbanni.shop.product.dto;

import java.math.BigDecimal;

public record FilterProductRequestDto(
        BigDecimal min,
        BigDecimal max
) {
}
