package com.mbanni.shop.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequestDto(
        @NotNull(message = "Quantity is required")
        @Min(value = -1, message = "Quantity change must be at least -1")
        @Max(value = 1, message = "Quantity change must be at most 1")
        Integer quantity
) {}