package com.careerpilot.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates the JWT on every request that isn't explicitly public, and - if
 * valid - forwards the caller's identity to downstream services as plain
 * headers (X-User-Id, X-User-Email, X-User-Role) so they don't each have to
 * re-parse the token just to know who's calling.
 *
 * This is a first line of defense, not the only one: each downstream service
 * will still enforce its own role-based authorization (e.g. "only the
 * employer who owns this job can see its applicants") independently, the
 * same way the current .NET API does with [Authorize(Roles = ...)]. The
 * gateway can't know about per-resource ownership rules, only "is this a
 * valid, non-expired token".
 *
 * Note on claim names: the current .NET AuthService signs tokens with
 * System.Security.Claims.ClaimTypes constants, which JwtSecurityToken
 * serializes as long schema URIs (e.g.
 * "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier")
 * rather than short claim names. Since auth-service is being rebuilt from
 * scratch in this migration, the new tokens use plain short claims instead
 * (`sub`, `email`, `role`) - there's no legacy token population to stay
 * compatible with, so there's no reason to carry the old quirk forward.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    static final List<PublicRoute> PUBLIC_ROUTES = List.of(
            new PublicRoute(HttpMethod.POST, "/api/auth/register"),
            new PublicRoute(HttpMethod.POST, "/api/auth/login"),
            new PublicRoute(HttpMethod.POST, "/api/auth/refresh-token"),
            new PublicRoute(HttpMethod.POST, "/api/auth/forgot-password"),
            new PublicRoute(HttpMethod.POST, "/api/auth/reset-password"),
            // Logout must be reachable with an expired access token, otherwise
            // the refresh token can never be revoked once the access token
            // lapses. auth-service decides what to revoke; see its
            // AuthController.logout().
            new PublicRoute(HttpMethod.POST, "/api/auth/logout"),
            new PublicRoute(HttpMethod.GET, "/api/jobs"),
            new PublicRoute(HttpMethod.GET, "/api/jobs/*")
    );

    /**
     * Identity headers this gateway sets on the way through. They are stripped
     * from every INBOUND request first - see filter() - so a client can never
     * inject them.
     */
    private static final String[] IDENTITY_HEADERS = {"X-User-Id", "X-User-Email", "X-User-Role"};

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // CORS preflight carries no credentials and must never be challenged.
        // Spring Cloud Gateway normally answers OPTIONS before global filters
        // run, so this is belt-and-braces - but "preflight got a 401" is the
        // single most common way this setup breaks, and the guard is free.
        if (HttpMethod.OPTIONS.equals(request.getMethod())) {
            return chain.filter(exchange);
        }

        // HEADER SANITIZATION. X-User-Id / X-User-Email / X-User-Role are
        // trusted identity assertions for everything downstream, so they must
        // originate here and nowhere else. Strip anything the caller sent
        // BEFORE the public-route short-circuit, otherwise a request to a
        // public route (e.g. GET /api/jobs) would carry attacker-supplied
        // identity headers straight through to a service. Today every service
        // re-validates the JWT itself and ignores these headers, so this is
        // not currently exploitable - it stops it from ever becoming so.
        ServerHttpRequest sanitizedRequest = stripIdentityHeaders(request);
        ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

        if (isPublic(sanitizedRequest)) {
            return chain.filter(sanitizedExchange);
        }

        String authHeader = sanitizedRequest.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(sanitizedExchange, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);

        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutatedRequest = sanitizedRequest.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Email", claims.get("email", String.class))
                    .header("X-User-Role", claims.get("role", String.class))
                    .build();

            return chain.filter(sanitizedExchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            return reject(sanitizedExchange, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            return reject(sanitizedExchange, "Invalid token");
        }
    }

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        boolean present = false;
        for (String header : IDENTITY_HEADERS) {
            if (request.getHeaders().containsKey(header)) {
                present = true;
                break;
            }
        }
        if (!present) {
            return request;
        }
        return request.mutate().headers(headers -> {
            for (String header : IDENTITY_HEADERS) {
                headers.remove(header);
            }
        }).build();
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        return PUBLIC_ROUTES.stream().anyMatch(route -> route.matches(method, path));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"success\":false,\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

}
