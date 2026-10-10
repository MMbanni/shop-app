package com.mbanni.shop.cart.dto;

import com.mbanni.shop.common.exception.ErrorCode;

public record CartItemProblem(
        ErrorCode code,
        Long cartItemId,
        Long productId,
        Integer stock,
        Integer requestedQuantity,
        Integer maximumQuantity,
        String detail
) {
}
