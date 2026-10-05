package com.mbanni.shop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.mbanni.shop.common.Constants.*;

public record UpdateUserRequestDto (
        @Size(min = MIN_NAME_LENGTH, max = MAX_NAME_LENGTH, message = "Name must be between 2 and 50 characters")
        String name,
        @Email(message = "Email must be valid")
        @Size(max = MAX_EMAIL_LENGTH, message = "Email must be at most 100 characters")
        String email
){
}


