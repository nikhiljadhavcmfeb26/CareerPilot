package com.careerpilot.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Cloud Config Server.
 *
 * This is the one service in the platform that does NOT pull its own
 * configuration from the config server, for the obvious reason that it
 * IS the config server. It boots entirely from application.yml on its
 * own classpath, then serves every other service's application.yml
 * (eureka-server.yml, api-gateway.yml, auth-service.yml, ...) out of the
 * git-backed config repo defined in spring.cloud.config.server.git.uri.
 *
 * Startup order for the whole platform: config-server -> eureka-server ->
 * api-gateway -> business services.
 */
@EnableConfigServer
@SpringBootApplication
public class ConfigServerApplication {

    private static final String CONFIG_REPO_URI = "CONFIG_REPO_URI";
    private static final String CONFIG_REPO_DIR = "careerpilot-config-repo";
    private static final int MAX_PARENT_LEVELS = 6;

    public static void main(String[] args) {
        resolveConfigRepoUri();
        SpringApplication.run(ConfigServerApplication.class, args);
    }

    /**
     * The git URI used to be hard-coded to one machine's absolute Windows path
     * ("file:///D:/CDAC PGCP-AC/Job Portal Final Project/..."), which broke the
     * whole platform on any other machine, folder, or OS - and the unescaped
     * spaces in that URI were fragile on their own.
     *
     * Resolution order, highest priority first:
     *   1. CONFIG_REPO_URI environment variable or -DCONFIG_REPO_URI system
     *      property - use this to point at a real git remote in a deployed
     *      environment.
     *   2. Auto-discovery: walk up from the current working directory looking
     *      for a "careerpilot-config-repo" folder. This works whether you run
     *      from the project root, from careerpilot-backend, from the
     *      config-server module, or from an IDE run configuration.
     *   3. Neither found -> leave it unset, and application.yml's own default
     *      produces a clear "cannot clone repo" failure rather than a silent
     *      half-boot.
     *
     * Path.toUri() also URI-encodes the result, so directory names containing
     * spaces are handled correctly.
     */
    private static void resolveConfigRepoUri() {
        if (System.getProperty(CONFIG_REPO_URI) != null || System.getenv(CONFIG_REPO_URI) != null) {
            return;
        }

        Path dir = Paths.get("").toAbsolutePath();
        for (int level = 0; level <= MAX_PARENT_LEVELS && dir != null; level++, dir = dir.getParent()) {
            Path candidate = dir.resolve(CONFIG_REPO_DIR);
            if (Files.isDirectory(candidate)) {
                System.setProperty(CONFIG_REPO_URI, candidate.toUri().toString());
                System.out.println("[config-server] Using config repo: " + candidate);
                return;
            }
        }

        System.err.println("[config-server] Could not locate a '" + CONFIG_REPO_DIR
                + "' folder near " + Paths.get("").toAbsolutePath()
                + ". Set the CONFIG_REPO_URI environment variable to your config repository.");
    }
}
