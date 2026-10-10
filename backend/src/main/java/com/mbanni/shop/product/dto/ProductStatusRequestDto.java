package com.mbanni.shop.product.dto;

import com.mbanni.shop.product.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record ProductStatusRequestDto(
        @NotNull(message = "Status required")
        ProductStatus status
){}

