package com.careerpilot.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One OpenAPI definition shared by every business service, rather than six
 * near-identical copies. Each service picks this up through its component scan
 * (scanBasePackages = "com.careerpilot") and titles its own document from
 * spring.application.name.
 *
 * The bearerAuth scheme is what makes Swagger UI actually usable here: almost
 * every endpoint requires the JWT that auth-service issues, so without an
 * "Authorize" button you could only ever exercise /api/auth/login.
 *
 * Reachable per service on its own port, e.g.
 * http://localhost:8081/swagger-ui.html for auth-service. Deliberately NOT
 * routed through the gateway - see the note in each SecurityConfig.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:careerpilot}")
    private String applicationName;

    @Bean
    public OpenAPI careerPilotOpenApi() {
        final String schemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("CareerPilot - " + applicationName)
                        .version("1.0.0")
                        .description("""
                                CareerPilot AI-powered job portal.

                                Obtain a token from auth-service (POST /api/auth/login), then click \
                                Authorize and paste the accessToken value. Endpoints under \
                                /internal/** are service-to-service (OpenFeign) and are not intended \
                                to be called directly."""))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
