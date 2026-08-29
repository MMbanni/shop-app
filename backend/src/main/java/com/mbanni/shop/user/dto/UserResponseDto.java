package com.mbanni.shop.user.dto;

import com.mbanni.shop.user.Role;
import com.mbanni.shop.user.UserStatus;

public record UserResponseDto(
    Long id,
    String email,
    String name,
    Role role,
    UserStatus status
){}
