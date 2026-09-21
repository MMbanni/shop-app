package com.mbanni.shop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto (
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        String name,
        @Email(message = "Email must be valid")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email
){}


