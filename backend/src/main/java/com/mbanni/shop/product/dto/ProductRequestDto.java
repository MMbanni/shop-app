package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record ProductRequestDto (
        @NotBlank(message = "Name required")
        String name,
        @Min(value = 0, message = "Min 0")
        Integer stock,
        @Min(value = 0, message = "Min 0")
        BigDecimal price,
        String description
){}
