package com.mbanni.shop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 100)

        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 72)
        String password
) {}
