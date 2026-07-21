package com.mbanni.shop.product.command;

import java.math.BigDecimal;

public record UpdateProductCommand(
        String name,
        Integer stock,
        BigDecimal price
) {}
