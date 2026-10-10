package com.mbanni.shop.checkout.dto;

public record FinalizeCheckoutDto(
        Long orderId,
        String sessionId,
        String sessionUrl
) {
}
