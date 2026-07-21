package com.mbanni.shop.cart.dto;

import com.mbanni.shop.cart.Cart;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RemoveFromCartRequestDto(
        @NotNull
        Long cartItemId,

        @Min(1) @Max(Cart.MAX_QUANTITY)
        int quantity
) {}