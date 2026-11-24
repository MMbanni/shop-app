package com.mbanni.shop.user;

import com.mbanni.shop.user.dto.*;
import com.mbanni.shop.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;
    private final EmailChangeService emailChangeService;


    public UserController(UserService userService, UserMapper userMapper, EmailChangeService emailChangeService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.emailChangeService = emailChangeService;
    }

    // Requires admin role
    @GetMapping
    public List<UserResponseDto> getUsers() {
        return userService.searchUsers("ALL");
    }

    // Requires admin role
    @GetMapping("/{status}")
    public List<UserResponseDto> getUsers(@PathVariable String status) {
        return userService.searchUsers(status);
    }

    @GetMapping("/me")
    public UserResponseDto getUser(Authentication authentication) {
        Long userId = getCurrentUserId(authentication);

        return userService.getUser(userId);
    }

    @PatchMapping("/me")
    public UserResponseDto updateUser(Authentication authentication, @Valid @RequestBody UpdateUserRequestDto request) {
        Long userId = getCurrentUserId(authentication);

        return userService.updateUserInfo(
                userId,
                userMapper.toCommand(request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequestDto request) {

        userService.updateUserStatus(
                id,
                userMapper.toCommand(request)
        );
        return ResponseEntity.noContent().build();

    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

    @PostMapping("/me/email/request")
    public ResponseEntity<Void> requestEmail(Authentication auth, @Valid @RequestBody EmailRequestDto body, HttpServletRequest request) {
        emailChangeService.request(getCurrentUserId(auth), body.email(), body.password(), request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/email/confirm")
    public ResponseEntity<Void> confirmEmail(Authentication auth, @Valid @RequestBody Verification body) {
        emailChangeService.confirm(getCurrentUserId(auth), body.code());
        return ResponseEntity.noContent().build();
    }

}

