package com.mbanni.shop.cart.dto;

import java.math.BigDecimal;

public record ConfirmPriceRequestDto(
        BigDecimal agreedPrice
) {
}
