package com.mbanni.shop.auth.command;

public record RegisterUserCommand(
        String name,
        String email,
        String password
) {}