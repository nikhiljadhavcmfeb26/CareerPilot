package com.careerpilot.authservice.dto;

import java.math.BigDecimal;

/**
 * Read-only view of the single configured paid plan, assembled from the
 * razorpay.* properties auth-service already reads. Nothing new is stored -
 * this exists so the frontend pricing card renders the amount the user will
 * actually be charged rather than a hard-coded literal.
 *
 * A record, so Jackson emits the component names verbatim.
 */
public record PlanResponse(
        String planName,
        long amountPaise,
        BigDecimal amount,
        String currency,
        long durationDays) {
}
