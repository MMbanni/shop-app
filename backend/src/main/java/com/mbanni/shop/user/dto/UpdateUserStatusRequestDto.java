package com.mbanni.shop.user.dto;

import com.mbanni.shop.user.UserStatus;

public record UpdateUserStatusRequestDto(
        UserStatus status,
        int duration
) {
}
