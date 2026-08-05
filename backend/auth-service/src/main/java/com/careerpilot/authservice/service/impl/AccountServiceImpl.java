package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.dto.ChangePasswordRequest;
import com.careerpilot.authservice.dto.UpdateUserRequest;
import com.careerpilot.authservice.dto.UserDto;
import com.careerpilot.authservice.dto.UserListDto;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.repository.RefreshTokenRepository;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.AccountService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    public AccountServiceImpl(UserRepository userRepository, UserMapper userMapper,
                               PasswordEncoder passwordEncoder, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public UserDto getCurrentUser(int userId) {
        User user = findUser(userId);
        return userMapper.toUserDto(user);
    }

    @Override
    @Transactional
    public UserDto updateProfile(int userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        userRepository.save(user);
        return userMapper.toUserDto(user);
    }

    /**
     * Reuses the same BCrypt PasswordEncoder bean and RefreshTokenRepository
     * that AuthServiceImpl already uses - no new hashing or session machinery.
     */
    @Override
    @Transactional
    public void changePassword(int userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect.");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password must be different from the current one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Changing a password should end any other live session. Revoking the
        // refresh token means the old one can no longer be exchanged for a new
        // access token; the caller's current access token stays valid until it
        // expires, same as every other stateless-JWT system.
        refreshTokenRepository.findByUserId(userId).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    public List<UserListDto> getAllUsers() {
        return userRepository.findAllWithRoles().stream().map(this::toListDto).toList();
    }

    @Override
    @Transactional
    public void deactivateUser(int userId) {
        User user = findUser(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void activateUser(int userId) {
        User user = findUser(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private UserListDto toListDto(User user) {
        UserListDto dto = new UserListDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole().getName());
        dto.setActive(user.isActive());
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
