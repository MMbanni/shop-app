package com.mbanni.shop.user.command;

import com.mbanni.shop.user.UserStatus;

public record UpdateUserStatusCommand(
        UserStatus status,
        int duration
) {
}
