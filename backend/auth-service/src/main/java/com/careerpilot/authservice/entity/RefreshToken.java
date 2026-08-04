package com.careerpilot.authservice.entity;

import com.careerpilot.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * SINGLE-DEVICE SESSIONS ARE INTENTIONAL (documented, not a bug).
 *
 * The unique constraint on user_id means each user has exactly one refresh
 * token row, so logging in on a second device silently invalidates the first.
 * That is the behaviour this platform ships with, and it is a deliberate
 * trade-off rather than an oversight:
 *
 *   - It makes "log out everywhere" and account deactivation trivially
 *     correct - there is exactly one row to revoke.
 *   - It matches the .NET application this was migrated from.
 *   - Supporting concurrent devices means dropping the unique constraint,
 *     switching to @ManyToOne, and changing every findByUserId call site to
 *     handle a collection. That is a schema + repository change, out of scope
 *     for an authentication bug-fix pass.
 *
 * Tokens ARE rotated on every refresh (generateAuthResponse writes a new
 * value into this row), so a captured refresh token is invalidated as soon as
 * the legitimate client next refreshes.
 *
 * If multi-device login is wanted later, that is a self-contained change to
 * this entity, RefreshTokenRepository, and AuthServiceImpl - flagging it here
 * so the constraint is a decision on record rather than an accident.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private boolean isRevoked;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isRevoked() {
        return isRevoked;
    }

    public void setRevoked(boolean revoked) {
        isRevoked = revoked;
    }
}
