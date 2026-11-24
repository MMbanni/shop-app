package com.mbanni.shop.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Verification(
        @NotBlank
        @Size(max=100)
        String code
) {}
