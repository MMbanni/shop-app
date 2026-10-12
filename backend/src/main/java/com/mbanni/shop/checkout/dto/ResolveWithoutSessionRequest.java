package com.mbanni.shop.checkout.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveWithoutSessionRequest(
        @AssertTrue(message = "Confirm that no Stripe session exists")
        boolean confirmedNoSession,

        @NotBlank
        @Size(max = 800)
        String reason
) {}