package com.mbanni.shop.product.command;

import java.math.BigDecimal;
import java.util.Map;

public record UpdateProductCommand(
        String name,
        Integer stock,
        BigDecimal price,
        String description,
        String imageUrl,
        Map<String, String> attributes,
        Long expectedVersion
) {}
