package com.mbanni.shop.user.dto;

import com.mbanni.shop.user.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequestDto(
        @NotNull(message = "Status required")
        UserStatus status,
        int duration
) {
}
