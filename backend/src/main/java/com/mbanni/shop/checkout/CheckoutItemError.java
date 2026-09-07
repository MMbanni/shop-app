package com.mbanni.shop.checkout;

import com.mbanni.shop.common.exception.ErrorCode;

public record CheckoutItemError(
        Long cartItemId,
        String code,
        String detail,
        Integer stock
) {
}