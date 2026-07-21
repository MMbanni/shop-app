package com.mbanni.shop.auth;

import com.mbanni.shop.auth.dto.AuthResponseDto;
import com.mbanni.shop.auth.dto.LoginRequestDto;
import com.mbanni.shop.login.LoginRateLimiter;
import com.mbanni.shop.security.ClientIpService;
import com.mbanni.shop.user.User;
import com.mbanni.shop.auth.dto.RegisterUserRequestDto;
import com.mbanni.shop.user.dto.UserResponseDto;
import com.mbanni.shop.user.mapper.UserMapper;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UserMapper userMapper;
    private final LoginRateLimiter loginRateLimiter;
    private final ClientIpService clientIpService;

    public AuthController(
            AuthService authService,
            UserMapper userMapper,
            LoginRateLimiter loginRateLimiter,
            ClientIpService clientIpService
    ) {
        this.authService=authService;
        this.userMapper=userMapper;
        this.loginRateLimiter=loginRateLimiter;
        this.clientIpService=clientIpService;
    }

    @PostMapping("/register")
    public UserResponseDto register(@Valid @RequestBody RegisterUserRequestDto request) {
        User user = authService.register(userMapper.toCommand(request));
        return userMapper.toResponse(user);
    }

    @PostMapping("/login")
    public AuthResponseDto login(@Valid @RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        String ipAddress = clientIpService.getClientIp(httpRequest);
        loginRateLimiter.checkAllowed(request.email(), ipAddress);
        return authService.login(request);

    }
}
