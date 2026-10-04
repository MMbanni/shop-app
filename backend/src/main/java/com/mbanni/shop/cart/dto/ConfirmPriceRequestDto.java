package com.mbanni.shop.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfirmPriceRequestDto(
        @NotNull
        @DecimalMin("0.00")
        BigDecimal agreedPrice
) {
}
