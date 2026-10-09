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

import java.time.Clock;
import java.util.Locale;

import static com.mbanni.shop.common.Constants.MIN_NAME_LENGTH;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    // Inject repository, Jwt service and password encoder
    public AuthService(UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository=userRepository;
        this.jwtService=jwtService;
        this.passwordEncoder=passwordEncoder;
        this.clock=clock;
    }

    @Transactional
    public User register(RegisterUserCommand command) {

        // Keep letters English for email
        String email = command.email().trim().toLowerCase(Locale.ROOT);
        String name = command.name().trim();

        if(userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
        }

        if(name.length() < MIN_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
        }

        User user = new User();

        user.setEmail(email);
        user.setName(name);
        user.setStatus(UserStatus.ACTIVE);

        String hashedPassword = passwordEncoder.encode(command.password());
        user.setPassword(hashedPassword);

        user.assignCart(new Cart());

        return userRepository.save(user);
    }



    @Transactional
    public AuthResponseDto login (LoginRequestDto request ) {

        String email = request.email().trim().toLowerCase(Locale.ROOT);

        // Using method with pessimistic write because login may change user status
        User user = userRepository.findByEmailForUpdate(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));


        boolean match = passwordEncoder.matches(request.password(), user.getPassword());
        if(!match) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.checkSuspensionOrActivate(clock.instant());

        if (user.getStatus() == UserStatus.BANNED || user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        if(user.getStatus() == UserStatus.SUSPENDED) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        String token = jwtService.createToken(user);

        return new AuthResponseDto(token);

    }


}
