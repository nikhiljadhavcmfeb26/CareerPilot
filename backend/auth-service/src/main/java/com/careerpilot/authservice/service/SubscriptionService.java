package com.careerpilot.authservice.service;

import com.careerpilot.authservice.dto.CreateOrderRequest;
import com.careerpilot.authservice.dto.CreateOrderResponse;
import com.careerpilot.authservice.dto.PlanResponse;
import com.careerpilot.authservice.dto.SubscriptionResponse;
import com.careerpilot.authservice.dto.VerifyPaymentRequest;

/**
 * New service - the .NET app has no subscription/payment concept at all.
 * Implements the Premium Subscription requirements from your spec using
 * Razorpay Orders + payment signature verification.
 */
public interface SubscriptionService {

    /**
     * The configured plan, so the pricing card shows the real amount from
     * razorpay.premium-amount-paise instead of a number hard-coded in React
     * that would silently drift from what the user is actually charged.
     */
    PlanResponse getPlan();

    CreateOrderResponse createOrder(int userId, CreateOrderRequest request);

    SubscriptionResponse verifyPayment(int userId, VerifyPaymentRequest request);

    SubscriptionResponse getMySubscription(int userId);

    boolean isPremiumActive(int userId);

    void handleWebhook(String payload, String signatureHeader);
}
