package com.mbanni.shop.user;

import com.mbanni.shop.common.exception.BusinessException;
import com.mbanni.shop.common.exception.ErrorCode;
import com.mbanni.shop.user.command.UpdateUserCommand;
import com.mbanni.shop.user.dto.UserResponseDto;
import com.mbanni.shop.user.mapper.UserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }


    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsers() {
        List<User> list = userRepository.findAll();

        return userMapper.toResponseList(list);
    }

    @Transactional(readOnly = true)
    public UserResponseDto getUser(Long userId) {

        User user = findUserOrThrow(userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponseDto updateUserInfo(Long userId, UpdateUserCommand command) {
        User user = findUserOrThrow(userId);

        if(command.name() != null) {
            String name = command.name().trim();
            if(name.isEmpty()) throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
            user.setName(name);
        }
        if (command.email() != null) {
            String email = command.email().trim().toLowerCase(Locale.ROOT);

            if (email.isEmpty()) {
                throw new BusinessException(ErrorCode.ILLEGAL_OPERATION);
            }

            if (!email.equals(user.getEmail())
                    && userRepository.existsByEmail(email)) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_USED);
            }

            user.setEmail(email);
        }

        return userMapper.toResponse(user);
    }


    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        User user = findUserOrThrow(userId);
        userRepository.delete(user);
    }


    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

}
