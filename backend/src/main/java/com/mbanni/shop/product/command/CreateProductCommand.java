package com.mbanni.shop.product.command;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductCommand(
        String name,
        Integer stock,
        BigDecimal price,
        String description,
        String imageUrl,
        Map<String, String> attributes
){}
