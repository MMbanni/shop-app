package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequestDto(
        String name,
        @Min(0) Integer stock,
        @Min(0) BigDecimal price,
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
) {}
