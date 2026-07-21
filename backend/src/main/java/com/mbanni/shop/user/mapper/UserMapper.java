package com.mbanni.shop.user.mapper;

import com.mbanni.shop.user.User;
import com.mbanni.shop.auth.command.RegisterUserCommand;
import com.mbanni.shop.user.command.UpdateUserCommand;
import com.mbanni.shop.auth.dto.RegisterUserRequestDto;
import com.mbanni.shop.user.dto.UpdateUserRequestDto;
import com.mbanni.shop.user.dto.UserResponseDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponseDto toResponse(User user) {
        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    public List<UserResponseDto> toResponseList(List<User> users) {
        return users
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RegisterUserCommand toCommand(RegisterUserRequestDto request) {
        return new RegisterUserCommand(
                request.name(),
                request.email(),
                request.password()
        );
    }

    public UpdateUserCommand toCommand(UpdateUserRequestDto request) {
        return new UpdateUserCommand(
                request.name(),
                request.email()
        );
    }



}