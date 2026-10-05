package com.mbanni.shop.auth.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.mbanni.shop.common.Constants.MAX_EMAIL_LENGTH;
import static com.mbanni.shop.common.Constants.USER_MAX_NAME_LENGTH;

public record RegisterUserRequestDto(

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = USER_MAX_NAME_LENGTH, message = "Name must be between 2 and 50 characters")
    String name,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = MAX_EMAIL_LENGTH, message = "Email must be at most 100 characters")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @Pattern(
            regexp = "[\\x20-\\x7E]+",
            message = "Use English letters, numbers, spaces, or basic punctuation"
    )
    String password

) {}
