package com.careerpilot.apigateway.filter;

import org.springframework.http.HttpMethod;

/**
 * One entry in the gateway's public-route allowlist: a method plus a path
 * pattern that may optionally end in a single-segment wildcard.
 *
 * <h2>Bug fix</h2>
 *
 * "/*" now matches exactly ONE path segment, the way Spring's own path
 * matchers treat it.
 *
 * The previous implementation (an inline record inside
 * {@link JwtAuthenticationFilter}) did a bare {@code startsWith} on the
 * prefix, so the public route {@code GET /api/jobs/*} also matched
 * {@code GET /api/jobs/admin/all} and {@code GET /api/jobs/my} - both
 * privileged. Those endpoints were never actually exposed, because job-service
 * re-validates the JWT and enforces {@code hasRole()} itself, so the practical
 * effect was a confusing 403 where a clean 401 belonged. But the gateway was
 * waving unauthenticated requests through to admin URLs on the strength of a
 * wildcard that was never meant to cross a "/", and the next public route
 * added under a shared prefix would have turned that into a real hole.
 *
 * Extracted to its own (package-private) type so the matching rule can be unit
 * tested without standing up the reactive filter chain.
 */
record PublicRoute(HttpMethod method, String pathPattern) {

    boolean matches(HttpMethod requestMethod, String requestPath) {
        if (!method.equals(requestMethod)) {
            return false;
        }
        if (pathPattern.endsWith("/*")) {
            String prefix = pathPattern.substring(0, pathPattern.length() - 1);
            if (!requestPath.startsWith(prefix)) {
                return false;
            }
            String remainder = requestPath.substring(prefix.length());
            // Exactly one non-empty segment: no further "/" allowed.
            return !remainder.isEmpty() && remainder.indexOf('/') < 0;
        }
        return pathPattern.equals(requestPath);
    }
}
