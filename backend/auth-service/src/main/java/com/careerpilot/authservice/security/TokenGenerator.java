package com.careerpilot.authservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

@Component
public class TokenGenerator {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiry-minutes:60}")
    private long expiryMinutes;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * The caller passes the expiry in so that the exact same instant ends up in
     * both the signed token's exp claim and the AuthResponse.expiresAt field
     * the client sees. Computing it separately in two places let the two drift.
     */
    public String generateAccessToken(int userId, String email, String role, LocalDateTime expiresAt) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiry = Date.from(expiresAt.atZone(ZoneOffset.UTC).toInstant());

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public LocalDateTime getAccessTokenExpiry() {
        return LocalDateTime.now(ZoneOffset.UTC).plusMinutes(expiryMinutes);
    }
}
