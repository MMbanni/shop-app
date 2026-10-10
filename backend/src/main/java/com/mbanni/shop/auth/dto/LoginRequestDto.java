package com.mbanni.shop.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static com.mbanni.shop.common.Constants.MAX_EMAIL_LENGTH;
import static com.mbanni.shop.common.Constants.MAX_PASSWORD_LENGTH;

public record LoginRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = MAX_EMAIL_LENGTH)

        String email,

        @NotBlank(message = "Password is required")
        @Size(max = MAX_PASSWORD_LENGTH)
        String password
) {}
