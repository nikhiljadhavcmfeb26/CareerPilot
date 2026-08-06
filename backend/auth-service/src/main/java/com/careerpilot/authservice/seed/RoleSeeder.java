package com.careerpilot.authservice.seed;

import com.careerpilot.authservice.entity.Role;
import com.careerpilot.authservice.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The original Program.cs calls "DbSeeder.SeedAsync(scope.ServiceProvider,
 * passwordHasher)" on startup, but no DbSeeder class exists anywhere in the
 * uploaded project - not in source, not in the compiled bin/obj output. The
 * .NET project as delivered would not actually build as-is; this reference
 * points at nothing.
 *
 * Rather than guess at what that missing class might have done (a default
 * admin login, for instance, would be pure invention with no source to
 * confirm credentials or even whether one was intended), this seeder covers
 * only what's unambiguous: the three roles every registration and
 * [Authorize(Roles=...)] check in the codebase depends on by exact string
 * match (see CareerPilot.Shared.Constants.Roles). Without these rows,
 * registration fails immediately with "Invalid role" on a fresh database.
 */
@Component
@Order(10)
public class RoleSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RoleSeeder.class);
    private static final String[] REQUIRED_ROLES = {"Admin", "Employer", "JobSeeker"};

    private final RoleRepository roleRepository;

    public RoleSeeder(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        for (String roleName : REQUIRED_ROLES) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
    }
}
