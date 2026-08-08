package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.client.NotificationServiceClient;
import com.careerpilot.authservice.client.UserServiceClient;
import com.careerpilot.authservice.dto.AuthResponse;
import com.careerpilot.authservice.dto.ForgotPasswordRequest;
import com.careerpilot.authservice.dto.LoginRequest;
import com.careerpilot.authservice.dto.RegisterRequest;
import com.careerpilot.authservice.dto.ResetPasswordRequest;
import com.careerpilot.authservice.entity.RefreshToken;
import com.careerpilot.authservice.entity.Role;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.repository.RefreshTokenRepository;
import com.careerpilot.authservice.repository.RoleRepository;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.security.TokenGenerator;
import com.careerpilot.authservice.service.AuthService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final String ROLE_EMPLOYER = "Employer";
    private static final String ROLE_JOB_SEEKER = "JobSeeker";

    /**
     * Roles a user is allowed to grant themselves through public self-service
     * registration. "Admin" is deliberately NOT here - it is seeded/assigned
     * out of band, never claimed by the caller.
     */
    private static final Set<String> SELF_REGISTERABLE_ROLES = Set.of(ROLE_EMPLOYER, ROLE_JOB_SEEKER);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final UserMapper userMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                            RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder,
                            TokenGenerator tokenGenerator, UserServiceClient userServiceClient,
                            NotificationServiceClient notificationServiceClient, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.userServiceClient = userServiceClient;
        this.notificationServiceClient = notificationServiceClient;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered.");
        }

        // PRIVILEGE-ESCALATION FIX. /api/auth/register is a public endpoint and
        // previously looked the requested role up by name with no allowlist, so
        // a hand-crafted request with {"role":"Admin"} minted a full
        // administrator. The React form only ever offers JobSeeker/Employer,
        // which is exactly why this went unnoticed - the UI was the only
        // control. Validate server-side instead.
        if (request.getRole() == null || !SELF_REGISTERABLE_ROLES.contains(request.getRole())) {
            throw new BadRequestException("Invalid role.");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BadRequestException("Invalid role."));

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhone(request.getPhone());
        user.setRole(role);

        // Flush (not just save) so the generated id is available for the
        // profile-creation call below, while staying in the same transaction:
        // if that Feign call fails, everything - including this insert - rolls
        // back, so we never end up with a User row and no matching profile.
        user = userRepository.saveAndFlush(user);

        if (ROLE_EMPLOYER.equals(role.getName()) || ROLE_JOB_SEEKER.equals(role.getName())) {
            try {
                userServiceClient.createProfile(new UserServiceClient.CreateProfileRequest(user.getId(), role.getName()));
            } catch (Exception ex) {
                log.error("Failed to create profile for new user {} via user-service", user.getId(), ex);
                throw new BadRequestException("Registration could not be completed right now. Please try again.");
            }
        }

        sendRegistrationEmailBestEffort(user);

        return generateAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        // ADMIN MODULE: blocked is checked before deactivated so a blocked user
        // gets the accurate message, and both are checked before the password
        // so a suspended account cannot be probed for a valid password.
        if (user.isBlocked()) {
            throw new UnauthorizedException("This account has been blocked. Contact support for assistance.");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        return generateAuthResponse(user);
    }

    /**
     * @Transactional is required, not cosmetic: generateAuthResponse() reads
     * user.getRole() (a LAZY @ManyToOne) and writes the rotated refresh token.
     * Without a transaction this only worked by accident, via Spring Boot's
     * open-in-view session; it would break the moment OSIV is disabled.
     */
    @Override
    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token."));

        if (token.isRevoked() || token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired.");
        }

        // A deactivated account must not be able to keep minting access tokens
        // off a refresh token issued before it was deactivated. login() already
        // enforces this; refresh did not.
        if (token.getUser().isBlocked()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("This account has been blocked. Contact support for assistance.");
        }

        if (!token.getUser().isActive()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new UnauthorizedException("Account is deactivated.");
        }

        return generateAuthResponse(token.getUser());
    }

    @Override
    @Transactional
    public void logout(int userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(this::revoke);
    }

    /**
     * Logout previously required a still-valid access token, which meant that
     * once the 60-minute token expired the refresh token could never be
     * revoked and stayed usable for its full 7-day life. Revoking by refresh
     * token closes that window. Silently succeeds on an unknown token - logout
     * must never leak whether a given token exists.
     */
    @Override
    @Transactional
    public void logoutByRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(refreshToken).ifPresent(this::revoke);
    }

    private void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(user -> {
            // 6-digit OTP, 100000-999999 inclusive, matching the .NET behavior exactly.
            String otp = String.valueOf(100000 + secureRandom.nextInt(900000));

            user.setResetOtp(otp);
            user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);

            try {
                notificationServiceClient.sendOtpEmail(
                        new NotificationServiceClient.OtpEmailRequest(user.getEmail(), user.getFirstName(), otp));
            } catch (Exception ex) {
                log.error("Failed to send OTP email to {} via notification-service", user.getEmail(), ex);
            }
        });
        // Intentionally silent if the email isn't found - don't reveal which
        // emails are registered, exactly like the .NET ForgotPasswordAsync.
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid request."));

        if (user.getResetOtp() == null || user.getResetOtpExpiry() == null) {
            throw new BadRequestException("No password reset was requested.");
        }
        if (!user.getResetOtp().equals(request.getOtp())) {
            throw new BadRequestException("Invalid OTP.");
        }
        if (user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        // Compute the expiry ONCE and hand the same instant to both the signed
        // token and the response body. Previously each was computed from its
        // own LocalDateTime.now() call, so the "expiresAt" the client used to
        // schedule its refresh could drift from the token's real exp claim.
        LocalDateTime accessTokenExpiry = tokenGenerator.getAccessTokenExpiry();

        String accessToken = tokenGenerator.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().getName(), accessTokenExpiry);
        String refreshTokenValue = tokenGenerator.generateRefreshToken();

        RefreshToken token = refreshTokenRepository.findByUserId(user.getId()).orElse(null);
        if (token == null) {
            token = new RefreshToken();
            token.setUser(user);
        }
        token.setToken(refreshTokenValue);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        refreshTokenRepository.save(token);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue);
        response.setExpiresAt(accessTokenExpiry);
        response.setUser(userMapper.toUserDto(user));
        return response;
    }

    private void sendRegistrationEmailBestEffort(User user) {
        try {
            notificationServiceClient.sendRegistrationEmail(new NotificationServiceClient.RegistrationEmailRequest(
                    user.getEmail(), user.getFirstName(), user.getRole().getName()));
        } catch (Exception ex) {
            log.error("Failed to send registration email to {} via notification-service", user.getEmail(), ex);
        }
    }
}
