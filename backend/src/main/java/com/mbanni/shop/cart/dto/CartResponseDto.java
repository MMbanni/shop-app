package com.mbanni.shop.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponseDto(
        List<CartItemResponseDto> items,
        BigDecimal total
){}
