package com.mbanni.shop.auth;

import com.mbanni.shop.auth.command.RegisterUserCommand;
import com.mbanni.shop.auth.dto.LoginRequestDto;
import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.security.JwtService;
import com.mbanni.shop.user.User;
import com.mbanni.shop.user.UserRepository;
import com.mbanni.shop.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import static com.mbanni.shop.support.TestSetup.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    final UserRepository users = mock(UserRepository.class);
    final JwtService jwt = mock(JwtService.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final AuthService service = new AuthService(users, jwt, passwords, CLOCK);
    final LoginRequestDto login = new LoginRequestDto("CUSTOMER@EXAMPLE.TEST", "test-password");
    User user;

    @BeforeEach void setUp() {
        user = user();
        when(users.findByEmailForUpdate("customer@example.test"))
                .thenReturn(Optional.of(user));

        when(passwords.matches("test-password", user.getPassword()))
                .thenReturn(true);

        when(jwt.createToken(user))
                .thenReturn("test-token");
    }

    @Test void registrationNormalizesEmailHashesPasswordAndCreatesCart() {
        when(passwords.encode("test-password"))
                .thenReturn("encoded");

        when(users.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User registered = service.register(new RegisterUserCommand("  Customer  ", " TEST@EXAMPLE.TEST ", "test-password"));
        assertEquals("test@example.test", registered.getEmail());
        assertEquals("Customer", registered.getName());
        assertEquals("encoded", registered.getPassword());
        assertSame(registered, registered.getCart().getUser());
        assertEquals(UserStatus.ACTIVE, registered.getStatus());
    }

    @Test void wrongPasswordDoesNotCreateAToken() {
        when(passwords.matches(anyString(), anyString())).thenReturn(false);

        assertEquals(
                ErrorCode.INVALID_CREDENTIALS,
                assertThrows( BusinessException.class, () -> service.login(login)).getErrorCode()
        );

        verify(jwt, never()).createToken(any());
    }

    @Test void bannedUserCannotLogIn() {
        user.ban();
        assertThrows(BusinessException.class, () -> service.login(login));
        verify(jwt, never()).createToken(any());
    }

    @Test void expiredSuspensionIsClearedAtLogin() {
        user.suspend(1, NOW.minusSeconds(172800));
        user.setStatus(UserStatus.SUSPENDED);

        ReflectionTestUtils.setField(user, "suspendedUntil", NOW.minusSeconds(1));

        assertEquals("test-token", service.login(login).token());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getSuspendedUntil());
    }
}
