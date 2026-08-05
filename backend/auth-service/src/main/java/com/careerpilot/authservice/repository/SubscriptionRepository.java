package com.careerpilot.authservice.repository;

import com.careerpilot.authservice.entity.Subscription;
import com.careerpilot.authservice.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {

    List<Subscription> findByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * Admin module: the subscription-management table, newest first. Fetches
     * the user eagerly because the table shows the subscriber's email and name
     * for every row - without the join this is a textbook N+1, and with LAZY
     * @ManyToOne it would also throw once open-in-view is disabled.
     */
    @Query("select s from Subscription s join fetch s.user u join fetch u.role order by s.createdAt desc")
    List<Subscription> findAllByOrderByCreatedAtDesc();

    /**
     * Admin dashboard: how many distinct people currently hold Premium.
     * DISTINCT matters - one user can accumulate several PAID rows over time
     * (renewals), and counting rows would overstate the subscriber count.
     */
    @Query("""
            select count(distinct s.user.id) from Subscription s
            where s.paymentStatus = :status and s.expiryDate > :now""")
    int countDistinctUsersWithActivePremium(@Param("status") PaymentStatus status,
                                            @Param("now") LocalDateTime now);

    /**
     * Every currently-active Premium subscription, in one query.
     *
     * The admin user list needs a premium flag per row. Asking
     * findFirstByUserIdAndPaymentStatus... once per user turns a 500-user list
     * into 501 queries; this loads the (much smaller) set of live
     * subscriptions once and the caller indexes it by user id.
     */
    List<Subscription> findByPaymentStatusAndExpiryDateAfter(PaymentStatus paymentStatus, LocalDateTime now);

    Optional<Subscription> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(Integer userId);

    /**
     * The subscription that actually grants Premium right now: paid, and not
     * yet expired.
     *
     * This exists because "most recent row" is NOT the same thing as "active
     * subscription". createOrder() inserts a CREATED row before the user has
     * paid anything, so an already-Premium user who merely opened the payment
     * page became the owner of a newer, unpaid row - and every premium check
     * that looked at "the latest row" then reported them as not premium.
     */
    Optional<Subscription> findFirstByUserIdAndPaymentStatusAndExpiryDateAfterOrderByExpiryDateDesc(
            Integer userId, PaymentStatus paymentStatus, LocalDateTime now);
}
