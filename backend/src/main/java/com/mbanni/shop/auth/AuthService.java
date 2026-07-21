package com.mbanni.shop.auth;


import com.mbanni.shop.auth.dto.AuthResponseDto;
import com.mbanni.shop.auth.dto.LoginRequestDto;
import com.mbanni.shop.cart.Cart;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.security.JwtService;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.mbanni.shop.auth.command.RegisterUserCommand;
import com.mbanni.shop.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Inject repository, Jwt service and password encoder
    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.userRepository=userRepository;
        this.jwtService=jwtService;
        this.passwordEncoder=passwordEncoder;
    }

    @Transactional
    public User register(RegisterUserCommand command) {

        // Keep letters English for email
        String email = command.email().trim().toLowerCase(Locale.ROOT);

        if(userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
        }

        User user = new User();

        user.setEmail(email);
        user.setName(command.name().trim());

        String hashedPassword = passwordEncoder.encode(command.password());
        user.setPassword(hashedPassword);

        user.assignCart(new Cart());

        return userRepository.save(user);
    }



    @Transactional(readOnly = true)
    public AuthResponseDto login (LoginRequestDto request ) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        System.out.println(email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        boolean match = passwordEncoder.matches(request.password(), user.getPassword());
        if(!match) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if(user.getStatus() == UserStatus.SUSPENDED) {
            if(user.getSuspendedUntil().isBefore(LocalDateTime.now())){
                user.setStatus(UserStatus.ACTIVE);
            } else {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
            }
        }

        String token = jwtService.createToken(user);

        return new AuthResponseDto(token);

    }


}
