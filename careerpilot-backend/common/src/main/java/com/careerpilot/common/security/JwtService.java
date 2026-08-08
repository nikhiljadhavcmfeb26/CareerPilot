package com.careerpilot.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Validates JWTs issued by auth-service. Every business service depends on
 * this rather than trusting the X-User-* headers the gateway forwards -
 * the gateway check is a fast-fail edge filter, this is the authoritative
 * check, mirroring how the current .NET services all independently run
 * AddJwtBearer + [Authorize(Roles = ...)] rather than trusting an upstream
 * proxy blindly.
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    public AuthenticatedUser validate(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Integer userId = Integer.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);

        return new AuthenticatedUser(userId, email, role);
    }
}
