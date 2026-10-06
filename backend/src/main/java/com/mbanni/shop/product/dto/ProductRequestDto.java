package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

import static com.mbanni.shop.common.Constants.*;

public record ProductRequestDto (
        @NotBlank(message = "Name required")
        @Size(max = MAX_NAME_LENGTH)
        String name,
        @Min(value = 0, message = "Min 0")
        Integer stock,

        @NotNull
        @DecimalMin(value = MIN_PRICE_SEK, message = "Min 4")
        @DecimalMax(value = MAX_PRICE_SEK, message = "Min 999999.99")
        BigDecimal price,
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description
){}
