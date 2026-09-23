package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateProductRequestDto(
        String name,
        @Min(0) Integer stock,
        @Min(0) BigDecimal price,
        @Max(500)
        String description
) {}
