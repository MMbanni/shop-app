package com.mbanni.shop.auth.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.mbanni.shop.common.Constants.*;

public record RegisterUserRequestDto(

    @NotBlank(message = "Name is required")
    @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH, message = "Name must be between 2 and 50 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = MAX_EMAIL_LENGTH, message = "Email must be at most 100 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH, message = "Password must be between 8 and 72 characters")
    @Pattern(
            regexp = "[\\x20-\\x7E]+",
            message = "Use English letters, numbers, spaces, or basic punctuation"
    )
    String password

) {}
