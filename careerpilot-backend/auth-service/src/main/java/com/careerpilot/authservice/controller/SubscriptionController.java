package com.careerpilot.authservice.controller;

import com.careerpilot.authservice.dto.CreateOrderRequest;
import com.careerpilot.authservice.dto.CreateOrderResponse;
import com.careerpilot.authservice.dto.PlanResponse;
import com.careerpilot.authservice.dto.SubscriptionResponse;
import com.careerpilot.authservice.dto.VerifyPaymentRequest;
import com.careerpilot.authservice.service.SubscriptionService;
import com.careerpilot.common.dto.ApiResponse;
import com.careerpilot.common.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /** Drives the pricing card - the amount shown is the amount actually charged. */
    @GetMapping("/plans")
    public ApiResponse<PlanResponse> plans() {
        return ApiResponse.ok(subscriptionService.getPlan());
    }

    @PostMapping("/create-order")
    public ApiResponse<CreateOrderResponse> createOrder(@RequestBody(required = false) CreateOrderRequest request) {
        CreateOrderRequest body = request != null ? request : new CreateOrderRequest();
        return ApiResponse.ok(subscriptionService.createOrder(SecurityUtils.currentUserId(), body));
    }

    @PostMapping("/verify-payment")
    public ApiResponse<SubscriptionResponse> verifyPayment(@Valid @RequestBody VerifyPaymentRequest request) {
        return ApiResponse.ok(subscriptionService.verifyPayment(SecurityUtils.currentUserId(), request), "Payment verified, premium activated");
    }

    @GetMapping("/my")
    public ApiResponse<SubscriptionResponse> mySubscription() {
        return ApiResponse.ok(subscriptionService.getMySubscription(SecurityUtils.currentUserId()));
    }

    /**
     * Server-to-server Razorpay webhook. Not gateway-routed to a human, but
     * IS routed at /api/auth/subscriptions/webhook so Razorpay's own servers
     * (which don't have a JWT) can reach it - permitAll() in SecurityConfig,
     * integrity is enforced by the webhook signature instead of a bearer token.
     */
    @PostMapping("/webhook")
    public ApiResponse<Void> webhook(HttpServletRequest request,
                                        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        String payload = readBody(request);
        subscriptionService.handleWebhook(payload, signature);
        return ApiResponse.success(null);
    }

    private String readBody(HttpServletRequest request) {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
