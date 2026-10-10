package com.mbanni.shop.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReconcileRequestDto(
        @NotBlank
        @Size(max = 255)String sessionId) {}