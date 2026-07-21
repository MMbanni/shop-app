package com.mbanni.shop.checkout;

import com.mbanni.shop.common.exception.ErrorCode;

public record CheckoutItemError(
        Long cartItemId,
        String title,
        String detail,
        Integer stock
) {
}