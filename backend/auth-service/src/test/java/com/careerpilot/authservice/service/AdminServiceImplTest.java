package com.careerpilot.authservice.service;

import com.careerpilot.authservice.client.ApplicationServiceClient;
import com.careerpilot.authservice.client.JobServiceClient;
import com.careerpilot.authservice.client.UserServiceClient;
import com.careerpilot.authservice.dto.admin.AdminStatsDto;
import com.careerpilot.authservice.dto.admin.AdminUserDetailDto;
import com.careerpilot.authservice.dto.internal.AiAccessResponse;
import com.careerpilot.authservice.entity.AiFeatureSetting;
import com.careerpilot.authservice.entity.Role;
import com.careerpilot.authservice.entity.Subscription;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.enums.PaymentStatus;
import com.careerpilot.authservice.enums.PlanName;
import com.careerpilot.authservice.repository.AiFeatureSettingRepository;
import com.careerpilot.authservice.repository.RefreshTokenRepository;
import com.careerpilot.authservice.repository.SubscriptionRepository;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.impl.AdminServiceImpl;
import com.careerpilot.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the admin module's decision logic - the parts where getting
 * the order or the guard wrong has real consequences.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceImplTest {

    private static final int ADMIN_ID = 1;
    private static final int USER_ID = 2;

    @Mock private UserRepository userRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private AiFeatureSettingRepository aiFeatureSettingRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private UserServiceClient userServiceClient;

    private AdminServiceImpl adminService;
    private AiFeatureSetting settings;
    private User user;

    @BeforeEach
    void setUp() {
        adminService = new AdminServiceImpl(userRepository, subscriptionRepository, aiFeatureSettingRepository,
                refreshTokenRepository, jobServiceClient, applicationServiceClient, userServiceClient);

        settings = new AiFeatureSetting();
        when(aiFeatureSettingRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));

        user = jobSeeker(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserId(anyInt())).thenReturn(Optional.empty());
        whenNoActiveSubscription();
    }

    private User jobSeeker(int id) {
        Role role = new Role();
        role.setName("JobSeeker");
        User u = new User();
        u.setId(id);
        u.setEmail("seeker@example.com");
        u.setFirstName("Test");
        u.setLastName("Seeker");
        u.setRole(role);
        u.setActive(true);
        u.setBlocked(false);
        u.setAiEnabled(true);
        return u;
    }

    private void whenNoActiveSubscription() {
        when(subscriptionRepository.findFirstByUserIdAndPaymentStatusAndExpiryDateAfterOrderByExpiryDateDesc(
                anyInt(), any(PaymentStatus.class), any(LocalDateTime.class))).thenReturn(Optional.empty());
    }

    private void whenActiveSubscription() {
        Subscription s = new Subscription();
        s.setId(9);
        s.setUser(user);
        s.setPlanName(PlanName.PREMIUM);
        s.setPaymentStatus(PaymentStatus.PAID);
        s.setAmount(new BigDecimal("499.00"));
        s.setStartDate(LocalDateTime.now().minusDays(1));
        s.setExpiryDate(LocalDateTime.now().plusDays(29));
        when(subscriptionRepository.findFirstByUserIdAndPaymentStatusAndExpiryDateAfterOrderByExpiryDateDesc(
                anyInt(), any(PaymentStatus.class), any(LocalDateTime.class))).thenReturn(Optional.of(s));
    }

    // -------------------------------------------------------- AI gate ----

    @Test
    @DisplayName("premium user with everything switched on is allowed")
    void allowsPremiumUser() {
        whenActiveSubscription();

        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isTrue();
        assertThat(access.premium()).isTrue();
        assertThat(access.reason()).isNull();
    }

    @Test
    @DisplayName("non-premium user is denied with the subscription reason")
    void deniesNonPremium() {
        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isFalse();
        assertThat(access.reason()).contains("Premium");
    }

    @Test
    @DisplayName("turning off requirePremium opens AI to non-premium users")
    void requirePremiumCanBeDisabled() {
        settings.setRequirePremium(false);

        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isTrue();
        assertThat(access.premium()).isFalse();
    }

    @Test
    @DisplayName("master switch beats an active subscription")
    void masterSwitchBeatsPremium() {
        whenActiveSubscription();
        settings.setAiEnabled(false);

        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isFalse();
        assertThat(access.reason()).contains("disabled by the administrator");
    }

    @Test
    @DisplayName("a per-feature switch only affects that feature")
    void perFeatureSwitchIsScoped() {
        whenActiveSubscription();
        settings.setCoverLetterEnabled(false);

        assertThat(adminService.checkAiAccess(USER_ID, "COVER_LETTER").allowed()).isFalse();
        assertThat(adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK").allowed()).isTrue();
    }

    @Test
    @DisplayName("a per-account AI revocation beats an active subscription")
    void perUserRevocationBeatsPremium() {
        whenActiveSubscription();
        user.setAiEnabled(false);

        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isFalse();
        assertThat(access.reason()).contains("disabled for this account");
    }

    @Test
    @DisplayName("blocked is reported ahead of deactivated and ahead of premium")
    void blockedWinsOverEverything() {
        whenActiveSubscription();
        user.setBlocked(true);
        user.setActive(false);

        AiAccessResponse access = adminService.checkAiAccess(USER_ID, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isFalse();
        assertThat(access.reason()).contains("blocked");
    }

    @Test
    @DisplayName("an unknown user is denied rather than throwing")
    void unknownUserIsDenied() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        AiAccessResponse access = adminService.checkAiAccess(999, "RESUME_FEEDBACK");

        assertThat(access.allowed()).isFalse();
        assertThat(access.reason()).contains("not found");
    }

    @Test
    @DisplayName("an unrecognised feature name falls back to the master switch")
    void unknownFeatureFallsBackToMaster() {
        whenActiveSubscription();

        assertThat(adminService.checkAiAccess(USER_ID, "SOMETHING_NEW").allowed()).isTrue();

        settings.setAiEnabled(false);
        assertThat(adminService.checkAiAccess(USER_ID, "SOMETHING_NEW").allowed()).isFalse();
    }

    // ------------------------------------------------- self-lockout ------

    @Test
    @DisplayName("an admin cannot deactivate their own account")
    void adminCannotDeactivateSelf() {
        assertThatThrownBy(() -> adminService.setActive(ADMIN_ID, ADMIN_ID, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("your own administrator account");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("an admin cannot block their own account")
    void adminCannotBlockSelf() {
        assertThatThrownBy(() -> adminService.block(ADMIN_ID, ADMIN_ID, "oops"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("an admin cannot revoke their own AI access, but can restore it")
    void adminCannotRevokeOwnAiButCanRestore() {
        assertThatThrownBy(() -> adminService.setUserAiEnabled(ADMIN_ID, ADMIN_ID, false))
                .isInstanceOf(BadRequestException.class);

        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(jobSeeker(ADMIN_ID)));
        assertThat(adminService.setUserAiEnabled(ADMIN_ID, ADMIN_ID, true).aiEnabled()).isTrue();
    }

    // -------------------------------------------------- user lifecycle ---

    @Test
    @DisplayName("blocking records a reason, a timestamp, and kills the session")
    void blockRecordsReasonAndRevokesSession() {
        AdminUserDetailDto result = adminService.block(ADMIN_ID, USER_ID, "spam");

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.blockedReason()).isEqualTo("spam");
        assertThat(result.blockedAt()).isNotNull();
        verify(refreshTokenRepository).findByUserId(USER_ID);
    }

    @Test
    @DisplayName("blocking with no reason still records a default one")
    void blockDefaultsTheReason() {
        assertThat(adminService.block(ADMIN_ID, USER_ID, "  ").blockedReason())
                .isEqualTo("Blocked by administrator");
    }

    @Test
    @DisplayName("unblocking clears the reason and the timestamp")
    void unblockClearsState() {
        adminService.block(ADMIN_ID, USER_ID, "spam");

        AdminUserDetailDto result = adminService.unblock(ADMIN_ID, USER_ID);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.blockedReason()).isNull();
        assertThat(result.blockedAt()).isNull();
    }

    // ------------------------------------------------- subscriptions -----

    @Test
    @DisplayName("extending a live subscription adds to its remaining time, not to today")
    void extendPreservesRemainingTime() {
        Subscription s = new Subscription();
        s.setId(5);
        s.setUser(user);
        s.setPlanName(PlanName.PREMIUM);
        s.setPaymentStatus(PaymentStatus.PAID);
        s.setAmount(new BigDecimal("499.00"));
        LocalDateTime currentExpiry = LocalDateTime.now().plusDays(20);
        s.setExpiryDate(currentExpiry);
        s.setStartDate(LocalDateTime.now().minusDays(10));
        when(subscriptionRepository.findById(5)).thenReturn(Optional.of(s));

        adminService.extendSubscription(5, 30);

        assertThat(s.getExpiryDate()).isEqualTo(currentExpiry.plusDays(30));
    }

    @Test
    @DisplayName("extending an expired subscription starts from today")
    void extendExpiredStartsFromToday() {
        Subscription s = new Subscription();
        s.setId(6);
        s.setUser(user);
        s.setPlanName(PlanName.PREMIUM);
        s.setPaymentStatus(PaymentStatus.PAID);
        s.setAmount(new BigDecimal("499.00"));
        s.setExpiryDate(LocalDateTime.now().minusDays(5));
        when(subscriptionRepository.findById(6)).thenReturn(Optional.of(s));

        adminService.extendSubscription(6, 10);

        assertThat(s.getExpiryDate()).isAfter(LocalDateTime.now().plusDays(9));
        assertThat(s.getExpiryDate()).isBefore(LocalDateTime.now().plusDays(11));
    }

    @Test
    @DisplayName("an unpaid subscription can neither be revoked nor extended")
    void unpaidSubscriptionCannotBeManaged() {
        Subscription s = new Subscription();
        s.setId(7);
        s.setUser(user);
        s.setPlanName(PlanName.PREMIUM);
        s.setPaymentStatus(PaymentStatus.CREATED);
        when(subscriptionRepository.findById(7)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> adminService.revokeSubscription(7)).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> adminService.extendSubscription(7, 30)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("revoking expires the row rather than deleting it")
    void revokeExpiresRatherThanDeletes() {
        Subscription s = new Subscription();
        s.setId(8);
        s.setUser(user);
        s.setPlanName(PlanName.PREMIUM);
        s.setPaymentStatus(PaymentStatus.PAID);
        s.setAmount(new BigDecimal("499.00"));
        s.setExpiryDate(LocalDateTime.now().plusDays(20));
        when(subscriptionRepository.findById(8)).thenReturn(Optional.of(s));

        adminService.revokeSubscription(8);

        assertThat(s.getExpiryDate()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(s.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(subscriptionRepository, never()).delete(any());
    }

    // ------------------------------------------------------ dashboard ----

    @Test
    @DisplayName("a downstream service being unavailable degrades to zeros, it does not fail the page")
    void statsDegradeGracefully() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByRoleName(anyString())).thenReturn(3);
        when(userRepository.countByIsActiveTrue()).thenReturn(9);
        when(userRepository.countByIsBlockedTrue()).thenReturn(1);
        when(jobServiceClient.getStats()).thenThrow(new RuntimeException("job-service down"));
        when(applicationServiceClient.getStats()).thenReturn(new ApplicationServiceClient.ApplicationStats(7, 2, 1));
        when(userServiceClient.countPendingCompanies()).thenThrow(new RuntimeException("user-service down"));
        when(subscriptionRepository.countDistinctUsersWithActivePremium(any(), any())).thenReturn(4);

        AdminStatsDto stats = adminService.getStats();

        assertThat(stats.totalUsers()).isEqualTo(10);
        assertThat(stats.totalJobs()).isZero();
        assertThat(stats.pendingEmployerApprovals()).isZero();
        assertThat(stats.totalApplications()).isEqualTo(7);
        assertThat(stats.premiumSubscribers()).isEqualTo(4);
        assertThat(stats.blockedUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("the user list filters by role, activation and free-text search")
    void userListFilters() {
        User employer = jobSeeker(3);
        employer.getRole().setName("Employer");
        employer.setEmail("hiring@acme.com");
        employer.setFirstName("Ada");
        User inactive = jobSeeker(4);
        inactive.setEmail("dormant@example.com");
        inactive.setActive(false);

        when(userRepository.findAllWithRoles()).thenReturn(List.of(user, employer, inactive));
        // The list path resolves premium in bulk, not per user - see
        // AdminServiceImpl.activePremiumExpiryByUser().
        Subscription live = new Subscription();
        live.setUser(employer);
        live.setPlanName(PlanName.PREMIUM);
        live.setPaymentStatus(PaymentStatus.PAID);
        live.setExpiryDate(LocalDateTime.now().plusDays(15));
        when(subscriptionRepository.findByPaymentStatusAndExpiryDateAfter(any(), any()))
                .thenReturn(List.of(live));

        List<AdminUserDetailDto> all = adminService.getUsers(null, null, null);
        assertThat(all).hasSize(3);
        assertThat(all).filteredOn(AdminUserDetailDto::premium).hasSize(1);
        assertThat(all).filteredOn(u -> u.id() == 3).singleElement()
                .extracting(AdminUserDetailDto::premiumExpiresAt).isNotNull();

        // Exactly one query for premium status regardless of how many users.
        verify(subscriptionRepository, times(1)).findByPaymentStatusAndExpiryDateAfter(any(), any());

        assertThat(adminService.getUsers("Employer", null, null)).hasSize(1);
        assertThat(adminService.getUsers(null, false, null)).hasSize(1);
        assertThat(adminService.getUsers(null, null, "ACME")).hasSize(1);
        assertThat(adminService.getUsers(null, null, "ada")).hasSize(1);
        assertThat(adminService.getUsers(null, null, "nobody")).isEmpty();
    }
}
