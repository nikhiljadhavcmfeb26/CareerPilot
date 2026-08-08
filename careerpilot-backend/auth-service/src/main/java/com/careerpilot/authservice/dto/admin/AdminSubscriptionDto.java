package com.careerpilot.authservice.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One subscription row, joined with enough of the user to be readable in the admin table. */
public record AdminSubscriptionDto(
        Integer id,
        Integer userId,
        String userEmail,
        String userName,
        String role,
        String planName,
        String paymentStatus,
        BigDecimal amount,
        String razorpayOrderId,
        String razorpayPaymentId,
        LocalDateTime startDate,
        LocalDateTime expiryDate,
        boolean active,
        LocalDateTime createdAt) {
}
