package com.mbanni.shop.product.command;

import com.mbanni.shop.product.ProductImage;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record UpdateProductCommand(
        String name,
        Integer stock,
        BigDecimal price,
        String description,
        List<String> images,
        Map<String, String> attributes,
        Long expectedVersion
) {}
