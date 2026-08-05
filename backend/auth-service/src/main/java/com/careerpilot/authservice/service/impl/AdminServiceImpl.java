package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.client.ApplicationServiceClient;
import com.careerpilot.authservice.client.JobServiceClient;
import com.careerpilot.authservice.client.UserServiceClient;
import com.careerpilot.authservice.dto.admin.AdminStatsDto;
import com.careerpilot.authservice.dto.admin.AdminSubscriptionDto;
import com.careerpilot.authservice.dto.admin.AdminUserDetailDto;
import com.careerpilot.authservice.dto.admin.AiFeatureSettingsDto;
import com.careerpilot.authservice.dto.internal.AiAccessResponse;
import com.careerpilot.authservice.entity.AiFeatureSetting;
import com.careerpilot.authservice.entity.Subscription;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.enums.PaymentStatus;
import com.careerpilot.authservice.repository.AiFeatureSettingRepository;
import com.careerpilot.authservice.repository.RefreshTokenRepository;
import com.careerpilot.authservice.repository.SubscriptionRepository;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.AdminService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * ADMIN MODULE.
 *
 * All admin behaviour that operates on accounts, subscriptions and AI access
 * lives here, in the service that already owns those tables. Job and
 * application administration deliberately stay in job-service and
 * application-service respectively (they own that data); this class only
 * aggregates their counts for the dashboard, exactly like DashboardServiceImpl
 * already does.
 *
 * Two invariants are enforced throughout:
 *   1. An administrator can never deactivate, block, or strip AI from their
 *      own account - that is the classic way to lock every human out of the
 *      platform with one misclick.
 *   2. Administrators are never listed as subscribable/AI-gated users; the
 *      admin account has no Premium concept at all.
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private static final String ROLE_ADMIN = "Admin";
    private static final String ROLE_EMPLOYER = "Employer";
    private static final String ROLE_JOB_SEEKER = "JobSeeker";

    private static final JobServiceClient.JobStats NO_JOBS = new JobServiceClient.JobStats(0, 0);
    private static final ApplicationServiceClient.ApplicationStats NO_APPLICATIONS =
            new ApplicationServiceClient.ApplicationStats(0, 0, 0);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AiFeatureSettingRepository aiFeatureSettingRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final UserServiceClient userServiceClient;

    public AdminServiceImpl(UserRepository userRepository, SubscriptionRepository subscriptionRepository,
                             AiFeatureSettingRepository aiFeatureSettingRepository,
                             RefreshTokenRepository refreshTokenRepository,
                             JobServiceClient jobServiceClient,
                             ApplicationServiceClient applicationServiceClient,
                             UserServiceClient userServiceClient) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.aiFeatureSettingRepository = aiFeatureSettingRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jobServiceClient = jobServiceClient;
        this.applicationServiceClient = applicationServiceClient;
        this.userServiceClient = userServiceClient;
    }

    // ---------------------------------------------------------------- users

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDetailDto> getUsers(String role, Boolean active, String search) {
        String needle = (search == null || search.isBlank()) ? null : search.trim().toLowerCase(Locale.ROOT);

        // Load every live subscription once and index it, rather than asking
        // the database "is this user premium?" for each of N rows.
        Map<Integer, LocalDateTime> premiumExpiryByUser = activePremiumExpiryByUser();

        return userRepository.findAllWithRoles().stream()
                .filter(u -> role == null || role.isBlank() || u.getRole().getName().equalsIgnoreCase(role))
                .filter(u -> active == null || u.isActive() == active)
                .filter(u -> needle == null
                        || u.getEmail().toLowerCase(Locale.ROOT).contains(needle)
                        || (u.getFirstName() + " " + u.getLastName()).toLowerCase(Locale.ROOT).contains(needle))
                .map(u -> toDetail(u, premiumExpiryByUser.get(u.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserDetailDto getUser(int userId) {
        return toDetail(findUser(userId));
    }

    private Map<Integer, LocalDateTime> activePremiumExpiryByUser() {
        return subscriptionRepository
                .findByPaymentStatusAndExpiryDateAfter(PaymentStatus.PAID, LocalDateTime.now())
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getUser().getId(),
                        Subscription::getExpiryDate,
                        // A user can hold several live rows after a renewal -
                        // the one that matters is the one that expires last.
                        (a, b) -> a.isAfter(b) ? a : b));
    }

    @Override
    @Transactional
    public AdminUserDetailDto setActive(int actingAdminId, int userId, boolean active) {
        User user = findManageableUser(actingAdminId, userId,
                active ? "activate" : "deactivate");
        user.setActive(active);
        if (!active) {
            revokeSessions(userId);
        }
        userRepository.save(user);
        log.info("Admin {} {} user {}", actingAdminId, active ? "activated" : "deactivated", userId);
        return toDetail(user);
    }

    @Override
    @Transactional
    public AdminUserDetailDto block(int actingAdminId, int userId, String reason) {
        User user = findManageableUser(actingAdminId, userId, "block");
        user.setBlocked(true);
        user.setBlockedReason(reason == null || reason.isBlank() ? "Blocked by administrator" : reason.trim());
        user.setBlockedAt(LocalDateTime.now());
        // Blocking is a security action: end the live session immediately
        // rather than waiting for the refresh token to lapse.
        revokeSessions(userId);
        userRepository.save(user);
        log.warn("Admin {} BLOCKED user {} - {}", actingAdminId, userId, user.getBlockedReason());
        return toDetail(user);
    }

    @Override
    @Transactional
    public AdminUserDetailDto unblock(int actingAdminId, int userId) {
        User user = findUser(userId);
        user.setBlocked(false);
        user.setBlockedReason(null);
        user.setBlockedAt(null);
        userRepository.save(user);
        log.info("Admin {} unblocked user {}", actingAdminId, userId);
        return toDetail(user);
    }

    // -------------------------------------------------------- subscriptions

    @Override
    @Transactional(readOnly = true)
    public List<AdminSubscriptionDto> getSubscriptions(String status) {
        return subscriptionRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(s -> status == null || status.isBlank()
                        || s.getPaymentStatus().name().equalsIgnoreCase(status))
                .map(this::toSubscriptionDto)
                .toList();
    }

    @Override
    @Transactional
    public AdminSubscriptionDto revokeSubscription(int subscriptionId) {
        Subscription subscription = findSubscription(subscriptionId);

        if (subscription.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Only a paid subscription can be revoked.");
        }

        // Expire it rather than deleting it: the payment really happened, and
        // the row is the record of it. Setting expiry to now makes every
        // premium check (which asks for expiryDate > now) fail immediately.
        subscription.setExpiryDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);
        log.warn("Admin revoked subscription {} for user {}", subscriptionId, subscription.getUser().getId());
        return toSubscriptionDto(subscription);
    }

    @Override
    @Transactional
    public AdminSubscriptionDto extendSubscription(int subscriptionId, int days) {
        Subscription subscription = findSubscription(subscriptionId);

        if (subscription.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Only a paid subscription can be extended.");
        }

        LocalDateTime now = LocalDateTime.now();
        // Extend from whichever is later: an already-expired subscription
        // extends from today, a live one extends from its current expiry, so
        // the customer never silently loses the remainder of what they paid for.
        LocalDateTime base = (subscription.getExpiryDate() == null || subscription.getExpiryDate().isBefore(now))
                ? now
                : subscription.getExpiryDate();

        if (subscription.getStartDate() == null) {
            subscription.setStartDate(now);
        }
        subscription.setExpiryDate(base.plusDays(days));
        subscriptionRepository.save(subscription);
        log.info("Admin extended subscription {} by {} days (new expiry {})",
                subscriptionId, days, subscription.getExpiryDate());
        return toSubscriptionDto(subscription);
    }

    // ------------------------------------------------------------ AI access

    @Override
    @Transactional(readOnly = true)
    public AiFeatureSettingsDto getAiSettings() {
        return toSettingsDto(settings());
    }

    @Override
    @Transactional
    public AiFeatureSettingsDto updateAiSettings(AiFeatureSettingsDto request) {
        AiFeatureSetting entity = settings();
        entity.setAiEnabled(request.aiEnabled());
        entity.setResumeFeedbackEnabled(request.resumeFeedbackEnabled());
        entity.setCoverLetterEnabled(request.coverLetterEnabled());
        entity.setJobRecommendationsEnabled(request.jobRecommendationsEnabled());
        entity.setCandidateScreeningEnabled(request.candidateScreeningEnabled());
        entity.setRejectionFeedbackEnabled(request.rejectionFeedbackEnabled());
        entity.setRequirePremium(request.requirePremium());
        aiFeatureSettingRepository.save(entity);
        log.info("Admin updated AI feature settings: {}", request);
        return toSettingsDto(entity);
    }

    @Override
    @Transactional
    public AdminUserDetailDto setUserAiEnabled(int actingAdminId, int userId, boolean enabled) {
        User user = enabled ? findUser(userId) : findManageableUser(actingAdminId, userId, "revoke AI access from");
        user.setAiEnabled(enabled);
        userRepository.save(user);
        log.info("Admin {} set aiEnabled={} for user {}", actingAdminId, enabled, userId);
        return toDetail(user);
    }

    /**
     * The authoritative AI gate. Order matters - the most specific, most
     * security-relevant reason wins, so an operator reading the message knows
     * which switch to flip.
     */
    @Override
    @Transactional(readOnly = true)
    public AiAccessResponse checkAiAccess(int userId, String feature) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return AiAccessResponse.deny("Account not found.", false);
        }
        if (user.isBlocked()) {
            return AiAccessResponse.deny("This account is blocked.", false);
        }
        if (!user.isActive()) {
            return AiAccessResponse.deny("This account is deactivated.", false);
        }
        if (!user.isAiEnabled()) {
            return AiAccessResponse.deny("AI features have been disabled for this account by an administrator.", false);
        }

        AiFeatureSetting settings = settings();
        if (!settings.isAiEnabled()) {
            return AiAccessResponse.deny("AI features are currently disabled by the administrator.", false);
        }
        if (!isFeatureEnabled(settings, feature)) {
            return AiAccessResponse.deny("This AI feature is currently disabled by the administrator.", false);
        }

        boolean premium = activeSubscription(userId).isPresent();
        if (settings.isRequirePremium() && !premium) {
            return AiAccessResponse.deny("This feature requires an active Premium subscription.", false);
        }

        return AiAccessResponse.allow(premium);
    }

    private boolean isFeatureEnabled(AiFeatureSetting settings, String feature) {
        if (feature == null || feature.isBlank()) {
            return true;
        }
        return switch (feature.toUpperCase(Locale.ROOT)) {
            case "RESUME_FEEDBACK" -> settings.isResumeFeedbackEnabled();
            case "COVER_LETTER" -> settings.isCoverLetterEnabled();
            case "JOB_RECOMMENDATIONS" -> settings.isJobRecommendationsEnabled();
            case "CANDIDATE_SCREENING" -> settings.isCandidateScreeningEnabled();
            case "REJECTION_FEEDBACK" -> settings.isRejectionFeedbackEnabled();
            // An unrecognised feature name is a client bug, not an authorization
            // decision - fall back to the master switch rather than denying.
            default -> true;
        };
    }

    // ------------------------------------------------------------ dashboard

    @Override
    @Transactional(readOnly = true)
    public AdminStatsDto getStats() {
        int totalUsers = (int) userRepository.count();
        int jobSeekers = userRepository.countByRoleName(ROLE_JOB_SEEKER);
        int employers = userRepository.countByRoleName(ROLE_EMPLOYER);
        int admins = userRepository.countByRoleName(ROLE_ADMIN);
        int active = userRepository.countByIsActiveTrue();
        int blocked = userRepository.countByIsBlockedTrue();

        JobServiceClient.JobStats jobs = safe("job-service stats", jobServiceClient::getStats, NO_JOBS);
        ApplicationServiceClient.ApplicationStats applications =
                safe("application-service stats", applicationServiceClient::getStats, NO_APPLICATIONS);
        int pendingApprovals = safe("user-service pending companies", userServiceClient::countPendingCompanies, 0);

        int premiumSubscribers = subscriptionRepository
                .countDistinctUsersWithActivePremium(PaymentStatus.PAID, LocalDateTime.now());

        return new AdminStatsDto(totalUsers, jobSeekers, employers, admins, active, blocked,
                jobs.totalJobs(), jobs.publishedJobs(),
                applications.total(), applications.pending(), applications.shortlisted(),
                premiumSubscribers, pendingApprovals);
    }

    // -------------------------------------------------------------- helpers

    /**
     * Same degraded-mode policy as DashboardServiceImpl: a monitoring screen
     * showing one zero tile beats a monitoring screen that will not load.
     */
    private <T> T safe(String what, Supplier<T> call, T fallback) {
        try {
            return call.get();
        } catch (Exception ex) {
            log.warn("Admin stats: {} unavailable, showing zeros for that section", what, ex);
            return fallback;
        }
    }

    private AiFeatureSetting settings() {
        return aiFeatureSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> aiFeatureSettingRepository.save(new AiFeatureSetting()));
    }

    private Optional<Subscription> activeSubscription(int userId) {
        return subscriptionRepository
                .findFirstByUserIdAndPaymentStatusAndExpiryDateAfterOrderByExpiryDateDesc(
                        userId, PaymentStatus.PAID, LocalDateTime.now());
    }

    private void revokeSessions(int userId) {
        refreshTokenRepository.findByUserId(userId).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private User findUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    /** Guards invariant 1: an admin cannot lock themselves out. */
    private User findManageableUser(int actingAdminId, int userId, String action) {
        if (actingAdminId == userId) {
            throw new BadRequestException("You cannot " + action + " your own administrator account.");
        }
        return findUser(userId);
    }

    private Subscription findSubscription(int subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found."));
    }

    /** Single-user path: one extra query is fine here. */
    private AdminUserDetailDto toDetail(User user) {
        return toDetail(user, activeSubscription(user.getId()).map(Subscription::getExpiryDate).orElse(null));
    }

    /** List path: the caller has already resolved premium status in bulk. */
    private AdminUserDetailDto toDetail(User user, LocalDateTime premiumExpiresAt) {
        return new AdminUserDetailDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getPhone(),
                user.getRole().getName(), user.isActive(), user.isBlocked(), user.getBlockedReason(),
                user.getBlockedAt(), user.isAiEnabled(), premiumExpiresAt != null,
                premiumExpiresAt, user.getCreatedAt());
    }

    private AdminSubscriptionDto toSubscriptionDto(Subscription s) {
        User user = s.getUser();
        return new AdminSubscriptionDto(
                s.getId(), user.getId(), user.getEmail(),
                user.getFirstName() + " " + user.getLastName(), user.getRole().getName(),
                s.getPlanName().name(), s.getPaymentStatus().name(), s.getAmount(),
                s.getRazorpayOrderId(), s.getRazorpayPaymentId(),
                s.getStartDate(), s.getExpiryDate(), s.isActive(), s.getCreatedAt());
    }

    private AiFeatureSettingsDto toSettingsDto(AiFeatureSetting e) {
        return new AiFeatureSettingsDto(e.isAiEnabled(), e.isResumeFeedbackEnabled(), e.isCoverLetterEnabled(),
                e.isJobRecommendationsEnabled(), e.isCandidateScreeningEnabled(), e.isRejectionFeedbackEnabled(),
                e.isRequirePremium());
    }
}
