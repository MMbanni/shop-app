package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import static com.mbanni.shop.common.Constants.*;

public record ProductRequestDto (
        @NotBlank(message = "Name required")
        @Size(max = MAX_NAME_LENGTH)
        String name,
        @NotNull(message = "Stock required")
        @Min(value = 0, message = "Stock cannot be negative")
        @Max(value = 9999, message = "Stock cannot exceed 9999")
        Integer stock,

        @NotNull
        @Digits(integer = PRICE_PRECISION-SCALE, fraction = SCALE, message = "Price must have at most 6 integer digits and 2 decimal places")
        @DecimalMin(value = MIN_PRICE_SEK, message = "Min 4")
        @DecimalMax(value = MAX_PRICE_SEK, message = "Max 999999.99")
        BigDecimal price,
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
){}
