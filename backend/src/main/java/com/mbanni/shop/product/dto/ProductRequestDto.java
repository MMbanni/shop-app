package com.mbanni.shop.product.dto;

import java.math.BigDecimal;

public record ProductRequestDto (
        String name,
        Integer stock,
        BigDecimal price,
        String description
){}
