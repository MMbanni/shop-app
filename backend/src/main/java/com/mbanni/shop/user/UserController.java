package com.mbanni.shop.user;

import com.mbanni.shop.user.dto.UpdateUserRequestDto;
import com.mbanni.shop.user.dto.UserResponseDto;
import com.mbanni.shop.user.mapper.UserMapper;
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


    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // Requires admin role
    @GetMapping
    public List<UserResponseDto> getUsers() {
        return userService.getAllUsers();
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

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {

        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private Long getCurrentUserId(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }

}

