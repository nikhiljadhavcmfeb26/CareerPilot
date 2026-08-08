package com.careerpilot.authservice.service.impl;

import com.careerpilot.authservice.dto.CreateOrderRequest;
import com.careerpilot.authservice.dto.CreateOrderResponse;
import com.careerpilot.authservice.dto.PlanResponse;
import com.careerpilot.authservice.dto.SubscriptionResponse;
import com.careerpilot.authservice.dto.VerifyPaymentRequest;
import com.careerpilot.authservice.entity.Subscription;
import com.careerpilot.authservice.entity.User;
import com.careerpilot.authservice.enums.PaymentStatus;
import com.careerpilot.authservice.enums.PlanName;
import com.careerpilot.authservice.repository.SubscriptionRepository;
import com.careerpilot.authservice.repository.UserRepository;
import com.careerpilot.authservice.service.SubscriptionService;
import com.careerpilot.common.exception.BadRequestException;
import com.careerpilot.common.exception.ResourceNotFoundException;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * New service - the .NET app has no payment/subscription concept to port.
 * Standard Razorpay Orders flow: backend creates an order, the React client
 * opens Razorpay Checkout with that order id, and on success posts the
 * resulting payment id + signature back here for verification. The webhook
 * handler is a second, server-to-server confirmation path that doesn't rely
 * on the client actually calling back (covers the case where the browser
 * tab closes right after a successful payment).
 *
 * NOTE: I could not reach Maven Central or the Razorpay docs from this
 * sandbox (no network access) to double check the exact razorpay-java 1.4.7
 * method signatures used below (RazorpayClient.orders.create,
 * Utils.verifyPaymentSignature, Utils.verifyWebhookSignature, Order.get).
 * They match the SDK's well-established public shape from prior versions,
 * but please sanity-check this file against the SDK on your machine before
 * relying on it in production.
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    @Value("${razorpay.premium-amount-paise:49900}")
    private long premiumAmountPaise;

    @Value("${razorpay.premium-duration-days:30}")
    private long premiumDurationDays;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, UserRepository userRepository,
                                    RazorpayClient razorpayClient) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.razorpayClient = razorpayClient;
    }

    @Override
    public PlanResponse getPlan() {
        return new PlanResponse(PlanName.PREMIUM.name(), premiumAmountPaise,
                BigDecimal.valueOf(premiumAmountPaise, 2), "INR", premiumDurationDays);
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(int userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        // Paying twice while already Premium would take the user's money and,
        // before the activeSubscription() fix below, actively revoke the
        // subscription they were still paying for.
        activeSubscription(userId).ifPresent(active -> {
            throw new BadRequestException(
                    "You already have an active Premium subscription until " + active.getExpiryDate().toLocalDate() + ".");
        });

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", premiumAmountPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "sub_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8));

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlanName(PlanName.PREMIUM);
            subscription.setPaymentStatus(PaymentStatus.CREATED);
            subscription.setRazorpayOrderId(orderId);
            subscription.setAmount(BigDecimal.valueOf(premiumAmountPaise, 2));
            subscriptionRepository.save(subscription);

            return new CreateOrderResponse(orderId, keyId, premiumAmountPaise, "INR");
        } catch (RazorpayException ex) {
            log.error("Failed to create Razorpay order for user {}", userId, ex);
            throw new BadRequestException("Could not start the payment. Please try again.");
        }
    }

    @Override
    @Transactional
    public SubscriptionResponse verifyPayment(int userId, VerifyPaymentRequest request) {
        Subscription subscription = subscriptionRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new BadRequestException("No matching order found."));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new BadRequestException("This order does not belong to you.");
        }

        // Razorpay can confirm the same payment twice - once through the
        // browser callback that lands here, and once through the webhook.
        // Without this guard the second one re-runs activateSubscription() and
        // silently restarts the 30-day window from that moment.
        if (subscription.getPaymentStatus() == PaymentStatus.PAID) {
            return toResponse(subscription);
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        boolean valid;
        try {
            valid = Utils.verifyPaymentSignature(options, keySecret);
        } catch (RazorpayException ex) {
            log.error("Signature verification threw for order {}", request.getRazorpayOrderId(), ex);
            valid = false;
        }

        if (!valid) {
            subscription.setPaymentStatus(PaymentStatus.FAILED);
            subscriptionRepository.save(subscription);
            throw new BadRequestException("Payment verification failed.");
        }

        activateSubscription(subscription, request.getRazorpayPaymentId());
        return toResponse(subscription);
    }

    /**
     * Prefers the ACTIVE subscription over merely the newest row. An abandoned
     * CREATED order (the user opened the payment page and closed it) must not
     * be what the account page reports, and must not mask a subscription the
     * user is still paying for. Falls back to the latest row only when nothing
     * is active, so a FAILED or expired attempt is still visible.
     */
    @Override
    public SubscriptionResponse getMySubscription(int userId) {
        return activeSubscription(userId)
                .or(() -> subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId))
                .map(this::toResponse)
                .orElseGet(() -> {
                    SubscriptionResponse empty = new SubscriptionResponse();
                    empty.setPlanName(PlanName.FREE.name());
                    empty.setActive(false);
                    return empty;
                });
    }

    /**
     * THE PREMIUM GATE every AI endpoint depends on, via auth-service's
     * InternalController. Previously it asked for the most recent subscription
     * row and called isActive() on it - so a paying customer lost every AI
     * feature the moment they revisited the payment page, because that page
     * inserts a CREATED row. Now it asks the database for an active
     * subscription directly.
     */
    @Override
    public boolean isPremiumActive(int userId) {
        return activeSubscription(userId).isPresent();
    }

    private Optional<Subscription> activeSubscription(int userId) {
        return subscriptionRepository
                .findFirstByUserIdAndPaymentStatusAndExpiryDateAfterOrderByExpiryDateDesc(
                        userId, PaymentStatus.PAID, LocalDateTime.now());
    }

    @Override
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Razorpay webhook received but razorpay.webhook-secret is not configured - ignoring.");
            return;
        }

        try {
            boolean valid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            if (!valid) {
                log.warn("Razorpay webhook signature verification failed.");
                return;
            }
        } catch (RazorpayException ex) {
            log.error("Razorpay webhook signature verification threw", ex);
            return;
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event", "");

        if (!"payment.captured".equals(eventType)) {
            return;
        }

        JSONObject payment = event.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
        String orderId = payment.optString("order_id", null);
        String paymentId = payment.optString("id", null);

        if (orderId == null) {
            return;
        }

        subscriptionRepository.findByRazorpayOrderId(orderId).ifPresent(subscription -> {
            if (subscription.getPaymentStatus() != PaymentStatus.PAID) {
                activateSubscription(subscription, paymentId);
            }
        });
    }

    private void activateSubscription(Subscription subscription, String paymentId) {
        LocalDateTime now = LocalDateTime.now();
        subscription.setPaymentStatus(PaymentStatus.PAID);
        subscription.setRazorpayPaymentId(paymentId);
        subscription.setStartDate(now);
        subscription.setExpiryDate(now.plusDays(premiumDurationDays));
        subscriptionRepository.save(subscription);
    }

    private SubscriptionResponse toResponse(Subscription subscription) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(subscription.getId());
        response.setPlanName(subscription.getPlanName().name());
        response.setPaymentStatus(subscription.getPaymentStatus().name());
        response.setAmount(subscription.getAmount());
        response.setStartDate(subscription.getStartDate());
        response.setExpiryDate(subscription.getExpiryDate());
        response.setActive(subscription.isActive());
        return response;
    }
}
