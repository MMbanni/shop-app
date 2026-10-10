package com.mbanni.shop.cart.dto;

import java.math.BigDecimal;

public record CartItemResponseDto(
        Long cartItemId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal priceWhenAdded,
        BigDecimal price,
        boolean priceChanged,
        BigDecimal lineTotal
) {
}
