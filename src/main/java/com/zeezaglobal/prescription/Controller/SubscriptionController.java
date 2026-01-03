package com.zeezaglobal.prescription.Controller;


import com.stripe.exception.StripeException;

import com.zeezaglobal.prescription.DTO.SubscriptionDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/status")
    public ResponseEntity<SubscriptionDTO.SubscriptionStatusResponse> getSubscriptionStatus(
            @AuthenticationPrincipal Doctor doctor) {
        SubscriptionDTO.SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(doctor.getId());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/details")
    public ResponseEntity<SubscriptionDTO.SubscriptionResponse> getSubscriptionDetails(
            @AuthenticationPrincipal Doctor doctor) {
        SubscriptionDTO.SubscriptionResponse details = subscriptionService.getSubscriptionDetails(doctor.getId());
        return ResponseEntity.ok(details);
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> createCheckoutSession(
            @AuthenticationPrincipal Doctor doctor,
            @RequestBody SubscriptionDTO.CreateCheckoutSessionRequest request) {
        try {
            SubscriptionDTO.CheckoutSessionResponse response = subscriptionService.createCheckoutSession(
                    doctor.getId(), request);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to create checkout session for doctor {}: {}",
                    doctor.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to create checkout session: " + e.getMessage()));
        }
    }

    @PostMapping("/billing-portal")
    public ResponseEntity<?> getBillingPortal(
            @AuthenticationPrincipal Doctor doctor,
            @RequestBody Map<String, String> request) {
        try {
            String returnUrl = request.get("returnUrl");
            SubscriptionDTO.BillingPortalResponse response = subscriptionService.createBillingPortalSession(
                    doctor.getId(), returnUrl);
            return ResponseEntity.ok(response);
        } catch (StripeException e) {
            log.error("Failed to create billing portal for doctor {}: {}",
                    doctor.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to access billing portal: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelSubscription(
            @AuthenticationPrincipal Doctor doctor,
            @RequestBody SubscriptionDTO.CancelSubscriptionRequest request) {
        try {
            subscriptionService.cancelSubscription(doctor.getId(), request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", request.isImmediate()
                            ? "Subscription cancelled immediately"
                            : "Subscription will be cancelled at the end of the billing period"
            ));
        } catch (StripeException e) {
            log.error("Failed to cancel subscription for doctor {}: {}",
                    doctor.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to cancel subscription: " + e.getMessage()));
        }
    }

    @GetMapping("/check-access")
    public ResponseEntity<Map<String, Object>> checkAccess(
            @AuthenticationPrincipal Doctor doctor) {
        boolean hasAccess = subscriptionService.hasActiveSubscription(doctor.getId());
        SubscriptionDTO.SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(doctor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("hasAccess", hasAccess);
        response.put("status", status.getStatus());
        response.put("isTrialPeriod", status.isTrialPeriod());
        response.put("daysRemaining", status.getDaysRemaining());
        response.put("message", status.getMessage());

        return ResponseEntity.ok(response);
    }
    @PostMapping("/start-trial")
    public ResponseEntity<?> startFreeTrial(@AuthenticationPrincipal Doctor doctor) {
        try {
            SubscriptionDTO.SubscriptionStatusResponse existingStatus =
                    subscriptionService.getSubscriptionStatus(doctor.getId());

            // Prevent restarting trial if already used
            if (existingStatus.getStatus() != null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error", "Trial already used or subscription exists",
                                "currentStatus", existingStatus.getStatus()
                        ));
            }

            subscriptionService.initializeTrialForDoctor(doctor);
            SubscriptionDTO.SubscriptionStatusResponse status =
                    subscriptionService.getSubscriptionStatus(doctor.getId());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Your 3-month free trial has started!",
                    "status", status
            ));
        } catch (Exception e) {
            log.error("Failed to start trial for doctor {}: {}", doctor.getId(), e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to start trial: " + e.getMessage()));
        }
    }
    @GetMapping("/checkout-success")
    public ResponseEntity<Map<String, Object>> checkoutSuccess(
            @RequestParam("session_id") String sessionId,
            @AuthenticationPrincipal Doctor doctor) {
        SubscriptionDTO.SubscriptionStatusResponse status = subscriptionService.getSubscriptionStatus(doctor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thank you for subscribing to IndigoRx!");
        response.put("status", status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/pricing")
    public ResponseEntity<Map<String, Object>> getPricing() {
        Map<String, Object> pricing = new HashMap<>();
        pricing.put("trialDays", 90);
        pricing.put("yearlyPrice", 6000);
        pricing.put("currency", "INR");
        pricing.put("features", new String[]{
                "Unlimited prescriptions",
                "Patient management",
                "Digital signatures",
                "PDF reports",
                "Email notifications",
                "Priority support"
        });

        return ResponseEntity.ok(pricing);
    }
}