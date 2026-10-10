package com.mbanni.shop.checkout.dto;

import com.mbanni.shop.product.Product;

import java.math.BigDecimal;

public record ProductReservation(
        Long sourceCartItemId,
        Product product,
        int quantity,
        BigDecimal unitPrice
) {
}