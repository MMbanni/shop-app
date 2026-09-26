package com.mbanni.shop.checkout;

public record FinalizeCheckoutDto(
        Long orderId,
        String sessionId,
        String sessionUrl
) {
}
