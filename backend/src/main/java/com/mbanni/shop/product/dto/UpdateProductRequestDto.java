package com.mbanni.shop.product.dto;

import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Map;

import static com.mbanni.shop.common.Constants.*;

public record UpdateProductRequestDto(
        @Size(max = MAX_NAME_LENGTH)
        String name,

        @Min(value = 0, message = "Stock cannot be negative")
        @Max(value = MAX_STOCK, message = "Stock cannot exceed 9999")
        Integer stock,


        @Digits(integer = PRICE_PRECISION-SCALE, fraction = SCALE)
        @DecimalMin(value = MIN_PRICE_SEK, message = "Min 4")
        @DecimalMax(value = MAX_PRICE_SEK, message = "Max 999999.99")
        BigDecimal price,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        @Size(max = 2048, message = "Image URL must be at most 2048 characters")
        @URL(protocol = "https", message = "Image must be a valid HTTPS URL")
        String imageUrl,

        Map<String, String> attributes,

        @NotNull
        Long expectedVersion

) {}
