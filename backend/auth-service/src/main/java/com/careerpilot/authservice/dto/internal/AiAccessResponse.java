package com.careerpilot.authservice.dto.internal;

/**
 * The single answer ai-service needs before doing any work: may this user use
 * this AI feature right now, and if not, why not?
 *
 * Replaces the old boolean-only /internal/subscriptions/{id}/active check,
 * which could only express "not premium" and therefore could not carry the
 * admin module's new reasons (feature switched off platform-wide, AI revoked
 * for this account, account blocked).
 */
public record AiAccessResponse(boolean allowed, String reason, boolean premium) {

    public static AiAccessResponse allow(boolean premium) {
        return new AiAccessResponse(true, null, premium);
    }

    public static AiAccessResponse deny(String reason, boolean premium) {
        return new AiAccessResponse(false, reason, premium);
    }
}
