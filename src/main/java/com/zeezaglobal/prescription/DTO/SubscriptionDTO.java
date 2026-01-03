package com.zeezaglobal.prescription.DTO;



import com.zeezaglobal.prescription.Entities.Subscription;
import com.zeezaglobal.prescription.Entities.Subscription.SubscriptionPlan;
import com.zeezaglobal.prescription.Entities.Subscription.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SubscriptionDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionResponse {
        private Long id;  // Can be null for new doctors
        private Long doctorId;
        private String doctorName;
        private String doctorEmail;
        private Subscription.SubscriptionStatus status;  // Can be null
        private Subscription.SubscriptionPlan plan;  // Can be null
        private LocalDateTime trialStartDate;
        private LocalDateTime trialEndDate;
        private LocalDateTime subscriptionStartDate;
        private LocalDateTime subscriptionEndDate;
        private LocalDateTime nextBillingDate;
        private Integer amountPaid;
        private String currency;
        private boolean isActive;
        private long daysRemaining;
        private boolean trialExpired;
        private boolean requiresPayment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCheckoutSessionRequest {
        private String successUrl;
        private String cancelUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutSessionResponse {
        private String sessionId;
        private String checkoutUrl;
        private String publishableKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionStatusResponse {
        private boolean hasActiveSubscription;
        private SubscriptionStatus status;
        private SubscriptionPlan plan;
        private long daysRemaining;
        private boolean isTrialPeriod;
        private boolean trialExpired;
        private LocalDateTime expiryDate;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelSubscriptionRequest {
        private String reason;
        private boolean immediate; // If true, cancel immediately; if false, cancel at period end
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentHistoryResponse {
        private String paymentId;
        private Integer amount;
        private String currency;
        private String status;
        private LocalDateTime paymentDate;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillingPortalResponse {
        private String portalUrl;
    }
}