package com.mbanni.shop.user.dto;

import com.mbanni.shop.user.Role;

public record UserResponseDto(
    Long id,
    String email,
    String name,
    Role role
){}
