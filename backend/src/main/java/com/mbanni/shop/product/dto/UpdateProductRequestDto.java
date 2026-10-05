package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequestDto(
        String name,
        @Min(0) Integer stock,
        @Min(0) @Digits(integer = 10, fraction = 2) BigDecimal price,
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,
        @NotNull
        Long expectedVersion

) {}
