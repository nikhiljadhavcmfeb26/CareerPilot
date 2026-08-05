package com.careerpilot.authservice.seed;

import com.careerpilot.authservice.entity.AiFeatureSetting;
import com.careerpilot.authservice.entity.Role;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.repository.AiFeatureSettingRepository;
import com.careerpilot.authservice.repository.RoleRepository;
import com.careerpilot.authservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADMIN MODULE - creates the single hard-coded administrator account, and the
 * one AI-settings row the admin console reads.
 *
 * Design notes:
 *   - There is no admin self-registration. /api/auth/register only accepts
 *     Employer and JobSeeker (SELF_REGISTERABLE_ROLES in AuthServiceImpl), so
 *     the account can only come from here.
 *   - The password is BCrypt-hashed through the same PasswordEncoder bean the
 *     rest of the service uses. It is never stored or logged in plain text -
 *     only the email is logged.
 *   - Credentials are overridable with ADMIN_EMAIL / ADMIN_PASSWORD so a real
 *     deployment is not stuck with the documented default. The defaults match
 *     what README.md publishes.
 *   - Idempotent: on every restart it ensures the account exists, is active,
 *     is not blocked, and still holds the Admin role. It deliberately does NOT
 *     reset the password on an existing account - an operator who changed it
 *     would not expect a restart to undo that. Set ADMIN_RESET_PASSWORD=true
 *     for the "I locked myself out" case.
 *   - Runs after RoleSeeder (@Order) because it needs the Admin role row.
 */
@Component
@Order(20)
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private static final String ROLE_ADMIN = "Admin";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AiFeatureSettingRepository aiFeatureSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@careerpilot.com}")
    private String adminEmail;

    @Value("${admin.password:CareerPilot@123}")
    private String adminPassword;

    @Value("${admin.first-name:Platform}")
    private String adminFirstName;

    @Value("${admin.last-name:Administrator}")
    private String adminLastName;

    @Value("${admin.reset-password:false}")
    private boolean resetPassword;

    public AdminSeeder(UserRepository userRepository, RoleRepository roleRepository,
                        AiFeatureSettingRepository aiFeatureSettingRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.aiFeatureSettingRepository = aiFeatureSettingRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedAiSettings();
        seedAdmin();
    }

    private void seedAiSettings() {
        if (aiFeatureSettingRepository.findFirstByOrderByIdAsc().isEmpty()) {
            aiFeatureSettingRepository.save(new AiFeatureSetting());
            log.info("Seeded default AI feature settings (all features on, Premium required).");
        }
    }

    private void seedAdmin() {
        Role adminRole = roleRepository.findByName(ROLE_ADMIN).orElse(null);
        if (adminRole == null) {
            // RoleSeeder guarantees this row; if it is missing something is
            // badly wrong and inventing the role here would only hide it.
            log.error("Role '{}' is missing - cannot seed the administrator account.", ROLE_ADMIN);
            return;
        }

        String email = adminEmail.trim().toLowerCase();

        User admin = userRepository.findByEmail(email).orElse(null);
        if (admin == null) {
            admin = new User();
            admin.setEmail(email);
            admin.setFirstName(adminFirstName);
            admin.setLastName(adminLastName);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(adminRole);
            admin.setActive(true);
            admin.setBlocked(false);
            admin.setAiEnabled(true);
            userRepository.save(admin);
            log.info("Seeded administrator account: {} (password comes from ADMIN_PASSWORD, "
                    + "default documented in README.md).", email);
            return;
        }

        boolean changed = false;

        if (!ROLE_ADMIN.equals(admin.getRole().getName())) {
            admin.setRole(adminRole);
            changed = true;
        }
        // An administrator locked out of their own console is unrecoverable
        // without database access, so a restart always restores reachability.
        if (!admin.isActive()) {
            admin.setActive(true);
            changed = true;
        }
        if (admin.isBlocked()) {
            admin.setBlocked(false);
            admin.setBlockedReason(null);
            admin.setBlockedAt(null);
            changed = true;
        }
        if (resetPassword) {
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            changed = true;
            log.warn("admin.reset-password=true - the administrator password has been reset. "
                    + "Unset ADMIN_RESET_PASSWORD before the next restart.");
        }

        if (changed) {
            userRepository.save(admin);
            log.info("Administrator account {} repaired on startup.", email);
        } else {
            log.info("Administrator account {} already present.", email);
        }
    }
}
