package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

import static com.mbanni.shop.common.Constants.MAX_NAME_LENGTH;

public record ProductRequestDto (
        @NotBlank(message = "Name required")
        @Size(max = MAX_NAME_LENGTH)
        String name,
        @Min(value = 0, message = "Min 0")
        Integer stock,
        @NotNull
        @Min(value = 0, message = "Min 0")
        BigDecimal price,
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
){}
