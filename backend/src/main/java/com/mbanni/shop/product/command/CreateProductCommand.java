package com.mbanni.shop.product.command;

import java.math.BigDecimal;

public record CreateProductCommand(
        String name,
        Integer stock,
        BigDecimal price,
        String description
){}
