package com.careerpilot.apigateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for the gateway's public-route allowlist.
 *
 * The "/*" cases are the ones that mattered: before the fix, the wildcard was
 * a bare prefix match, so every privileged /api/jobs/... URL was treated as a
 * public route at the edge.
 */
class PublicRouteTest {

    private static final PublicRoute JOB_BY_ID = new PublicRoute(HttpMethod.GET, "/api/jobs/*");
    private static final PublicRoute JOB_SEARCH = new PublicRoute(HttpMethod.GET, "/api/jobs");
    private static final PublicRoute LOGIN = new PublicRoute(HttpMethod.POST, "/api/auth/login");

    @Test
    @DisplayName("wildcard matches exactly one segment")
    void wildcardMatchesOneSegment() {
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/42")).isTrue();
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/abc")).isTrue();
    }

    @Test
    @DisplayName("wildcard does NOT cross a slash - the bug this fixes")
    void wildcardDoesNotCrossSlash() {
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/admin/all")).isFalse();
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/1/applicants")).isFalse();
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/admin/1/delete")).isFalse();
    }

    @Test
    @DisplayName("wildcard requires a non-empty segment")
    void wildcardRequiresSegment() {
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs/")).isFalse();
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobs")).isFalse();
    }

    @Test
    @DisplayName("wildcard does not match an unrelated prefix")
    void wildcardRespectsPrefix() {
        assertThat(JOB_BY_ID.matches(HttpMethod.GET, "/api/jobseekers/1")).isFalse();
    }

    @Test
    @DisplayName("exact patterns match the whole path only")
    void exactPatterns() {
        assertThat(JOB_SEARCH.matches(HttpMethod.GET, "/api/jobs")).isTrue();
        assertThat(JOB_SEARCH.matches(HttpMethod.GET, "/api/jobs/1")).isFalse();
        assertThat(LOGIN.matches(HttpMethod.POST, "/api/auth/login")).isTrue();
    }

    @Test
    @DisplayName("method must match")
    void methodMustMatch() {
        assertThat(JOB_BY_ID.matches(HttpMethod.DELETE, "/api/jobs/42")).isFalse();
        assertThat(LOGIN.matches(HttpMethod.GET, "/api/auth/login")).isFalse();
    }

    @Test
    @DisplayName("no admin route is reachable through the configured allowlist")
    void noAdminRouteIsPublic() {
        String[] adminPaths = {
                "/api/admin/stats", "/api/admin/users", "/api/admin/subscriptions",
                "/api/admin/ai-settings", "/api/jobs/admin/all", "/api/jobs/my",
                "/api/applications/admin/all", "/api/companies/admin/all"
        };
        for (String path : adminPaths) {
            for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE}) {
                boolean anyPublic = JwtAuthenticationFilter.PUBLIC_ROUTES.stream()
                        .anyMatch(route -> route.matches(method, path));
                assertThat(anyPublic)
                        .as("%s %s must not be treated as a public route", method, path)
                        .isFalse();
            }
        }
    }
}
