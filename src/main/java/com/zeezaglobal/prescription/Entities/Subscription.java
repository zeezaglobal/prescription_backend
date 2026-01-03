package com.zeezaglobal.prescription.Entities;



import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false, unique = true)
    private Doctor doctor;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionPlan plan = SubscriptionPlan.TRIAL;

    @Column(name = "trial_start_date")
    private LocalDateTime trialStartDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(name = "subscription_start_date")
    private LocalDateTime subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "next_billing_date")
    private LocalDateTime nextBillingDate;

    @Column(name = "amount_paid")
    private Integer amountPaid; // Amount in paise (6000 rupees = 600000 paise)

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "payment_method_id")
    private String paymentMethodId;

    @Column(name = "last_payment_date")
    private LocalDateTime lastPaymentDate;

    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to check if subscription is active
    public boolean isActive() {
        if (status == SubscriptionStatus.ACTIVE) {
            return subscriptionEndDate == null || subscriptionEndDate.isAfter(LocalDateTime.now());
        }
        if (status == SubscriptionStatus.TRIAL) {
            return trialEndDate != null && trialEndDate.isAfter(LocalDateTime.now());
        }
        return false;
    }

    // Helper method to check if trial has expired
    public boolean isTrialExpired() {
        return status == SubscriptionStatus.TRIAL &&
                trialEndDate != null &&
                trialEndDate.isBefore(LocalDateTime.now());
    }

    // Helper method to get days remaining in trial
    public long getTrialDaysRemaining() {
        if (status != SubscriptionStatus.TRIAL || trialEndDate == null) {
            return 0;
        }
        long days = java.time.Duration.between(LocalDateTime.now(), trialEndDate).toDays();
        return Math.max(0, days);
    }

    // Helper method to get days remaining in subscription
    public long getSubscriptionDaysRemaining() {
        if (status != SubscriptionStatus.ACTIVE || subscriptionEndDate == null) {
            return 0;
        }
        long days = java.time.Duration.between(LocalDateTime.now(), subscriptionEndDate).toDays();
        return Math.max(0, days);
    }

    public enum SubscriptionStatus {
        TRIAL,           // Currently in free trial
        ACTIVE,          // Paid subscription active
        PAST_DUE,        // Payment failed, grace period
        CANCELLED,       // Subscription cancelled
        EXPIRED,         // Trial or subscription expired
        PENDING          // Awaiting first payment
    }

    public enum SubscriptionPlan {
        TRIAL,           // 3-month free trial
        YEARLY           // ₹6000/year
    }
}