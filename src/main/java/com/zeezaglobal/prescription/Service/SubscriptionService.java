package com.zeezaglobal.prescription.Service;

import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.param.*;
import com.stripe.param.checkout.SessionCreateParams;
import com.zeezaglobal.prescription.Config.StripeConfig;
import com.zeezaglobal.prescription.DTO.SubscriptionDTO;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import com.zeezaglobal.prescription.Repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zeezaglobal.prescription.Entities.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final DoctorRepository doctorRepository;
    private final StripeConfig stripeConfig;

    // ==================== EXISTING METHODS ====================

    @Transactional
    public Subscription initializeTrialForDoctor(Doctor doctor) {
        log.info("=== INITIALIZE TRIAL START === Doctor ID: {}, Name: {}", doctor.getId(), doctor.getName());

        Optional<Subscription> existing = subscriptionRepository.findByDoctor(doctor);
        log.info("Existing subscription found: {}", existing.isPresent());

        if (existing.isPresent()) {
            Subscription sub = existing.get();
            log.info("Existing subscription details - ID: {}, Status: {}, TrialStartDate: {}",
                    sub.getId(), sub.getStatus(), sub.getTrialStartDate());
            if (sub.getTrialStartDate() != null) {
                log.warn("Trial already used for doctor {}", doctor.getId());
                throw new RuntimeException("Trial has already been used for this account");
            }
            return sub;
        }

        Subscription subscription = new Subscription();
        subscription.setDoctor(doctor);
        subscription.setStatus(Subscription.SubscriptionStatus.TRIAL);
        subscription.setPlan(Subscription.SubscriptionPlan.TRIAL);
        subscription.setTrialStartDate(LocalDateTime.now());
        subscription.setTrialEndDate(LocalDateTime.now().plusDays(stripeConfig.getTrialPeriodDays()));

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("=== INITIALIZE TRIAL END === Created new trial subscription ID: {} for doctor {}",
                saved.getId(), doctor.getId());
        return saved;
    }

    public SubscriptionDTO.SubscriptionStatusResponse getSubscriptionStatus(Long doctorId) {
        log.info("=== GET SUBSCRIPTION STATUS === Doctor ID: {}", doctorId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);
        log.info("Subscription found: {}", subscriptionOpt.isPresent());

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for doctor {}", doctorId);
            return SubscriptionDTO.SubscriptionStatusResponse.builder()
                    .hasActiveSubscription(false)
                    .status(null)
                    .message("No subscription found. Please contact support.")
                    .build();
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("Subscription details - ID: {}, Status: {}, Plan: {}, StripeSubId: {}, StripeCustomerId: {}",
                subscription.getId(), subscription.getStatus(), subscription.getPlan(),
                subscription.getStripeSubscriptionId(), subscription.getStripeCustomerId());
        log.info("Dates - TrialStart: {}, TrialEnd: {}, SubStart: {}, SubEnd: {}",
                subscription.getTrialStartDate(), subscription.getTrialEndDate(),
                subscription.getSubscriptionStartDate(), subscription.getSubscriptionEndDate());

        boolean isActive = subscription.isActive();
        boolean isTrialPeriod = subscription.getStatus() == Subscription.SubscriptionStatus.TRIAL;
        boolean trialExpired = subscription.isTrialExpired();

        log.info("Computed values - isActive: {}, isTrialPeriod: {}, trialExpired: {}",
                isActive, isTrialPeriod, trialExpired);

        long daysRemaining;
        LocalDateTime expiryDate;

        if (isTrialPeriod) {
            daysRemaining = subscription.getTrialDaysRemaining();
            expiryDate = subscription.getTrialEndDate();
        } else {
            daysRemaining = subscription.getSubscriptionDaysRemaining();
            expiryDate = subscription.getSubscriptionEndDate();
        }

        log.info("Days remaining: {}, Expiry date: {}", daysRemaining, expiryDate);

        String message = buildStatusMessage(subscription, isActive, isTrialPeriod, trialExpired, daysRemaining);

        SubscriptionDTO.SubscriptionStatusResponse response = SubscriptionDTO.SubscriptionStatusResponse.builder()
                .hasActiveSubscription(isActive)
                .status(subscription.getStatus())
                .plan(subscription.getPlan())
                .daysRemaining(daysRemaining)
                .isTrialPeriod(isTrialPeriod)
                .trialExpired(trialExpired)
                .expiryDate(expiryDate)
                .message(message)
                .build();

        log.info("=== GET SUBSCRIPTION STATUS END === Response: hasActive={}, status={}, message={}",
                response.isHasActiveSubscription(), response.getStatus(), response.getMessage());
        return response;
    }

    public SubscriptionDTO.SubscriptionResponse getSubscriptionDetails(Long doctorId) {
        log.info("=== GET SUBSCRIPTION DETAILS === Doctor ID: {}", doctorId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for doctor {}, returning empty response", doctorId);
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));

            return SubscriptionDTO.SubscriptionResponse.builder()
                    .id(null)
                    .doctorId(doctor.getId())
                    .doctorName(doctor.getName())
                    .doctorEmail(doctor.getEmail())
                    .status(null)
                    .plan(null)
                    .trialStartDate(null)
                    .trialEndDate(null)
                    .subscriptionStartDate(null)
                    .subscriptionEndDate(null)
                    .nextBillingDate(null)
                    .amountPaid(null)
                    .currency("INR")
                    .isActive(false)
                    .daysRemaining(0)
                    .trialExpired(false)
                    .requiresPayment(false)
                    .build();
        }

        Subscription subscription = subscriptionOpt.get();
        Doctor doctor = subscription.getDoctor();
        boolean isTrialPeriod = subscription.getStatus() == Subscription.SubscriptionStatus.TRIAL;

        log.info("=== GET SUBSCRIPTION DETAILS END === Returning details for subscription ID: {}, Status: {}",
                subscription.getId(), subscription.getStatus());

        return SubscriptionDTO.SubscriptionResponse.builder()
                .id(subscription.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getName())
                .doctorEmail(doctor.getEmail())
                .status(subscription.getStatus())
                .plan(subscription.getPlan())
                .trialStartDate(subscription.getTrialStartDate())
                .trialEndDate(subscription.getTrialEndDate())
                .subscriptionStartDate(subscription.getSubscriptionStartDate())
                .subscriptionEndDate(subscription.getSubscriptionEndDate())
                .nextBillingDate(subscription.getNextBillingDate())
                .amountPaid(subscription.getAmountPaid())
                .currency(subscription.getCurrency())
                .isActive(subscription.isActive())
                .daysRemaining(isTrialPeriod ? subscription.getTrialDaysRemaining() : subscription.getSubscriptionDaysRemaining())
                .trialExpired(subscription.isTrialExpired())
                .requiresPayment(subscription.isTrialExpired() || subscription.getStatus() == Subscription.SubscriptionStatus.EXPIRED)
                .build();
    }

    @Transactional
    public SubscriptionDTO.CheckoutSessionResponse createCheckoutSession(Long doctorId, SubscriptionDTO.CreateCheckoutSessionRequest request) throws StripeException {
        log.info("=== CREATE CHECKOUT SESSION START === Doctor ID: {}", doctorId);

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));
        log.info("Doctor found: {} ({})", doctor.getName(), doctor.getEmail());

        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseGet(() -> initializeTrialForDoctor(doctor));
        log.info("Subscription ID: {}, Current Status: {}", subscription.getId(), subscription.getStatus());

        String customerId = getOrCreateStripeCustomer(doctor, subscription);
        log.info("Stripe Customer ID: {}", customerId);

        SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(request.getSuccessUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(request.getCancelUrl())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(stripeConfig.getYearlyPriceId())
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("doctor_id", doctorId.toString())
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("doctor_id", doctorId.toString())
                                .build()
                );

        Session session = Session.create(sessionBuilder.build());
        log.info("=== CREATE CHECKOUT SESSION END === Session ID: {}, URL: {}", session.getId(), session.getUrl());

        return SubscriptionDTO.CheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .checkoutUrl(session.getUrl())
                .publishableKey(stripeConfig.getPublishableKey())
                .build();
    }

    public SubscriptionDTO.BillingPortalResponse createBillingPortalSession(Long doctorId, String returnUrl) throws StripeException {
        log.info("=== CREATE BILLING PORTAL SESSION === Doctor ID: {}", doctorId);

        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getStripeCustomerId() == null) {
            log.error("No Stripe customer found for doctor {}", doctorId);
            throw new RuntimeException("No Stripe customer found. Please subscribe first.");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(subscription.getStripeCustomerId())
                        .setReturnUrl(returnUrl)
                        .build();

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);

        log.info("Billing portal session created: {}", session.getUrl());
        return SubscriptionDTO.BillingPortalResponse.builder()
                .portalUrl(session.getUrl())
                .build();
    }

    // ==================== WEBHOOK HANDLER METHODS ====================

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) throws StripeException {
        log.info("========================================");
        log.info("=== CHECKOUT SESSION COMPLETED START ===");
        log.info("========================================");
        log.info("Session ID: {}", session.getId());
        log.info("Session Customer: {}", session.getCustomer());
        log.info("Session Subscription: {}", session.getSubscription());
        log.info("Session Metadata: {}", session.getMetadata());

        String doctorIdStr = session.getMetadata().get("doctor_id");
        if (doctorIdStr == null) {
            log.error("NO DOCTOR_ID IN CHECKOUT SESSION METADATA!");
            return;
        }

        Long doctorId = Long.parseLong(doctorIdStr);
        log.info("Doctor ID from metadata: {}", doctorId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);
        log.info("Subscription found in DB: {}", subscriptionOpt.isPresent());

        if (subscriptionOpt.isEmpty()) {
            log.error("SUBSCRIPTION NOT FOUND FOR DOCTOR: {}", doctorId);
            throw new RuntimeException("Subscription not found for doctor: " + doctorId);
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("BEFORE UPDATE - Subscription ID: {}, Status: {}, Plan: {}, StripeSubId: {}, StripeCustomerId: {}",
                subscription.getId(), subscription.getStatus(), subscription.getPlan(),
                subscription.getStripeSubscriptionId(), subscription.getStripeCustomerId());

        String stripeSubscriptionId = session.getSubscription();
        log.info("Retrieving Stripe subscription: {}", stripeSubscriptionId);

        com.stripe.model.Subscription stripeSubscription =
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);
        log.info("Stripe Subscription Status: {}", stripeSubscription.getStatus());
        log.info("Stripe Subscription Current Period End: {}", stripeSubscription.getCurrentPeriodEnd());

        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        LocalDateTime subscriptionEndDate = currentPeriodEnd != null
                ? LocalDateTime.ofEpochSecond(currentPeriodEnd, 0, java.time.ZoneOffset.UTC)
                : LocalDateTime.now().plusYears(1);
        log.info("Calculated subscription end date: {}", subscriptionEndDate);

        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStripeCustomerId(session.getCustomer());
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setSubscriptionEndDate(subscriptionEndDate);
        subscription.setNextBillingDate(subscriptionEndDate);
        subscription.setAmountPaid(stripeConfig.getYearlyAmountPaise());
        subscription.setLastPaymentDate(LocalDateTime.now());
        subscription.setCurrency("INR");

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("AFTER UPDATE - Subscription ID: {}, Status: {}, Plan: {}, StripeSubId: {}, StripeCustomerId: {}",
                saved.getId(), saved.getStatus(), saved.getPlan(),
                saved.getStripeSubscriptionId(), saved.getStripeCustomerId());
        log.info("========================================");
        log.info("=== CHECKOUT SESSION COMPLETED END ===");
        log.info("========================================");
    }

    @Transactional
    public void handleSubscriptionCreated(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION CREATED START ===");
        log.info("Stripe Subscription ID: {}", stripeSubscription.getId());
        log.info("Stripe Customer ID: {}", stripeSubscription.getCustomer());
        log.info("Stripe Status: {}", stripeSubscription.getStatus());

        // Most of the work is done in checkout.session.completed
        // This is mainly for logging and ensuring subscription is linked
        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);

        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            if (subscription.getStripeSubscriptionId() == null) {
                subscription.setStripeSubscriptionId(stripeSubscription.getId());
                subscriptionRepository.save(subscription);
                log.info("Linked Stripe subscription ID to local subscription");
            }
        }

        log.info("=== SUBSCRIPTION CREATED END ===");
    }

    @Transactional
    public void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        log.info("========================================");
        log.info("=== SUBSCRIPTION UPDATED START ===");
        log.info("========================================");

        String subscriptionId = stripeSubscription.getId();
        String customerId = stripeSubscription.getCustomer();
        String stripeStatus = stripeSubscription.getStatus();

        log.info("Stripe Subscription ID: {}", subscriptionId);
        log.info("Stripe Customer ID: {}", customerId);
        log.info("Stripe Status: {}", stripeStatus);

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);

        if (subscriptionOpt.isEmpty()) {
            log.warn("NO LOCAL SUBSCRIPTION FOUND! SubscriptionId: {}, CustomerId: {}", subscriptionId, customerId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("BEFORE UPDATE - Local Subscription ID: {}, Status: {}, Plan: {}",
                subscription.getId(), subscription.getStatus(), subscription.getPlan());

        // Update subscription end date from Stripe
        updateSubscriptionDates(subscription, stripeSubscription);

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("AFTER UPDATE - Local Subscription ID: {}, Status: {}, Plan: {}",
                saved.getId(), saved.getStatus(), saved.getPlan());
        log.info("=== SUBSCRIPTION UPDATED END ===");
    }

    @Transactional
    public void handleSubscriptionActivated(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION ACTIVATED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for activation");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);

        if (subscription.getSubscriptionStartDate() == null) {
            subscription.setSubscriptionStartDate(LocalDateTime.now());
        }

        updateSubscriptionDates(subscription, stripeSubscription);
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION ACTIVATED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionPastDue(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION PAST DUE START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for past_due update");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION PAST DUE END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionUnpaid(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION UNPAID START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for unpaid update");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION UNPAID END === Doctor: {}, Status set to EXPIRED", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionCanceled(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION CANCELED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for cancellation");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setCancellationDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION CANCELED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionCancellationScheduled(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION CANCELLATION SCHEDULED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for cancellation scheduling");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        // Status stays ACTIVE until actual cancellation
        // Just record that cancellation is scheduled
        Long cancelAt = stripeSubscription.getCancelAt();
        if (cancelAt != null) {
            LocalDateTime cancelDate = LocalDateTime.ofEpochSecond(cancelAt, 0, java.time.ZoneOffset.UTC);
            subscription.setSubscriptionEndDate(cancelDate);
            log.info("Cancellation scheduled for: {}", cancelDate);
        }
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION CANCELLATION SCHEDULED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION DELETED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for deleted Stripe subscription: {}", stripeSubscription.getId());
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
        subscription.setCancellationDate(LocalDateTime.now());
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION DELETED END === Doctor: {}, Status set to EXPIRED", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionPaused(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION PAUSED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for pause");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.PAUSED);
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION PAUSED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleSubscriptionResumed(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION RESUMED START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for resume");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        updateSubscriptionDates(subscription, stripeSubscription);
        subscriptionRepository.save(subscription);

        log.info("=== SUBSCRIPTION RESUMED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleInvoicePaymentSucceeded(Invoice invoice) {
        log.info("========================================");
        log.info("=== INVOICE PAYMENT SUCCEEDED START ===");
        log.info("========================================");

        String subscriptionId = invoice.getSubscription();
        String customerId = invoice.getCustomer();

        log.info("Invoice ID: {}", invoice.getId());
        log.info("Subscription ID: {}", subscriptionId);
        log.info("Customer ID: {}", customerId);
        log.info("Amount Paid: {}", invoice.getAmountPaid());
        log.info("Billing Reason: {}", invoice.getBillingReason());

        if (subscriptionId == null) {
            log.info("Invoice has no subscription ID, skipping");
            return;
        }

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeIds(subscriptionId, customerId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("NO LOCAL SUBSCRIPTION FOUND! SubscriptionId: {}, CustomerId: {}", subscriptionId, customerId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("BEFORE UPDATE - Subscription ID: {}, Status: {}", subscription.getId(), subscription.getStatus());

        // Link Stripe subscription ID if not already linked
        if (subscription.getStripeSubscriptionId() == null) {
            subscription.setStripeSubscriptionId(subscriptionId);
        }

        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);
        subscription.setLastPaymentDate(LocalDateTime.now());
        subscription.setAmountPaid(invoice.getAmountPaid() != null ? invoice.getAmountPaid().intValue() : 0);
        subscription.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "INR");

        // For renewal payments, extend the subscription
        String billingReason = invoice.getBillingReason();
        if ("subscription_cycle".equals(billingReason)) {
            // This is a renewal - extend by 1 year from current end date or now
            LocalDateTime currentEnd = subscription.getSubscriptionEndDate();
            LocalDateTime newEnd = (currentEnd != null && currentEnd.isAfter(LocalDateTime.now()))
                    ? currentEnd.plusYears(1)
                    : LocalDateTime.now().plusYears(1);
            subscription.setSubscriptionEndDate(newEnd);
            subscription.setNextBillingDate(newEnd);
            log.info("Renewal payment - Extended subscription to: {}", newEnd);
        } else if (subscription.getSubscriptionStartDate() == null) {
            subscription.setSubscriptionStartDate(LocalDateTime.now());
            subscription.setSubscriptionEndDate(LocalDateTime.now().plusYears(1));
            subscription.setNextBillingDate(LocalDateTime.now().plusYears(1));
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("AFTER UPDATE - Subscription ID: {}, Status: {}", saved.getId(), saved.getStatus());
        log.info("=== INVOICE PAYMENT SUCCEEDED END ===");
    }

    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        log.info("=== INVOICE PAYMENT FAILED START ===");

        String subscriptionId = invoice.getSubscription();
        String customerId = invoice.getCustomer();
        log.info("Subscription ID: {}, Customer ID: {}, Attempt: {}",
                subscriptionId, customerId, invoice.getAttemptCount());

        if (subscriptionId == null) {
            log.info("No subscription ID, skipping");
            return;
        }

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeIds(subscriptionId, customerId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for failed invoice");
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        // Only update to PAST_DUE if not already in a worse state
        if (subscription.getStatus() != Subscription.SubscriptionStatus.EXPIRED &&
                subscription.getStatus() != Subscription.SubscriptionStatus.CANCELLED) {
            subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
            subscriptionRepository.save(subscription);
        }

        log.info("=== INVOICE PAYMENT FAILED END === Doctor: {}", subscription.getDoctor().getId());
    }

    @Transactional
    public void handleTrialWillEnd(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== TRIAL WILL END START ===");

        Optional<Subscription> subscriptionOpt = findSubscriptionByStripeData(stripeSubscription);

        if (subscriptionOpt.isEmpty()) {
            log.info("Trial ending notification for unknown subscription: {}", stripeSubscription.getId());
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("=== TRIAL WILL END === Trial ending soon for doctor {}", subscription.getDoctor().getId());
        // Email is sent by the webhook controller
    }

    // ==================== HELPER METHODS FOR EMAIL ====================

    /**
     * Get doctor email by Stripe customer ID - used by webhook controller for emails
     */
    public String getEmailByStripeCustomerId(String stripeCustomerId) {
        if (stripeCustomerId == null) return null;

        return subscriptionRepository.findByStripeCustomerId(stripeCustomerId)
                .map(sub -> sub.getDoctor().getEmail())
                .orElse(null);
    }

    /**
     * Get doctor name by Stripe customer ID - used by webhook controller for emails
     */
    public String getNameByStripeCustomerId(String stripeCustomerId) {
        if (stripeCustomerId == null) return null;

        return subscriptionRepository.findByStripeCustomerId(stripeCustomerId)
                .map(sub -> sub.getDoctor().getName())
                .orElse(null);
    }

    // ==================== EXISTING METHODS CONTINUED ====================

    @Transactional
    public void cancelSubscription(Long doctorId, SubscriptionDTO.CancelSubscriptionRequest request) throws StripeException {
        log.info("=== CANCEL SUBSCRIPTION START === Doctor ID: {}, Immediate: {}", doctorId, request.isImmediate());

        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getStripeSubscriptionId() != null) {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());

            if (request.isImmediate()) {
                stripeSubscription.cancel();
                subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                log.info("Cancelled immediately");
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                stripeSubscription.update(params);
                log.info("Set to cancel at period end");
            }
        }

        subscription.setCancellationDate(LocalDateTime.now());
        subscription.setCancellationReason(request.getReason());

        if (request.isImmediate()) {
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        }

        subscriptionRepository.save(subscription);
        log.info("=== CANCEL SUBSCRIPTION END === Subscription cancelled for doctor {}", doctorId);
    }

    public boolean hasActiveSubscription(Long doctorId) {
        log.info("=== HAS ACTIVE SUBSCRIPTION CHECK === Doctor ID: {}", doctorId);
        boolean result = subscriptionRepository.hasActiveSubscription(doctorId, LocalDateTime.now());
        log.info("Result: {}", result);
        return result;
    }

    @Transactional
    public void updateExpiredTrials() {
        log.info("=== UPDATE EXPIRED TRIALS START ===");
        List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(LocalDateTime.now());
        log.info("Found {} expired trials", expiredTrials.size());

        for (Subscription subscription : expiredTrials) {
            subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            log.info("Trial expired for doctor {}", subscription.getDoctor().getId());
        }
        log.info("=== UPDATE EXPIRED TRIALS END ===");
    }

    public List<Subscription> getTrialsExpiringSoon(int daysBeforeExpiry) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(daysBeforeExpiry);
        return subscriptionRepository.findTrialsExpiringSoon(Subscription.SubscriptionStatus.TRIAL, start, end);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Find subscription by various Stripe identifiers
     */
    private Optional<Subscription> findSubscriptionByStripeData(com.stripe.model.Subscription stripeSubscription) {
        String subscriptionId = stripeSubscription.getId();
        String customerId = stripeSubscription.getCustomer();

        // Try subscription ID first
        Optional<Subscription> result = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        if (result.isPresent()) {
            return result;
        }

        // Try customer ID
        if (customerId != null) {
            result = subscriptionRepository.findByStripeCustomerId(customerId);
            if (result.isPresent()) {
                // Link the subscription ID for future lookups
                Subscription sub = result.get();
                sub.setStripeSubscriptionId(subscriptionId);
                return result;
            }
        }

        // Try metadata doctor_id
        String doctorIdStr = stripeSubscription.getMetadata().get("doctor_id");
        if (doctorIdStr != null) {
            Long doctorId = Long.parseLong(doctorIdStr);
            result = subscriptionRepository.findByDoctorId(doctorId);
            if (result.isPresent()) {
                Subscription sub = result.get();
                sub.setStripeSubscriptionId(subscriptionId);
                sub.setStripeCustomerId(customerId);
                return result;
            }
        }

        return Optional.empty();
    }

    /**
     * Find subscription by Stripe subscription ID or customer ID
     */
    private Optional<Subscription> findSubscriptionByStripeIds(String subscriptionId, String customerId) {
        Optional<Subscription> result = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        if (result.isEmpty() && customerId != null) {
            result = subscriptionRepository.findByStripeCustomerId(customerId);
        }
        return result;
    }

    /**
     * Update subscription dates from Stripe subscription
     */
    private void updateSubscriptionDates(Subscription subscription, com.stripe.model.Subscription stripeSubscription) {
        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        if (currentPeriodEnd != null) {
            LocalDateTime endDate = LocalDateTime.ofEpochSecond(currentPeriodEnd, 0, java.time.ZoneOffset.UTC);
            subscription.setSubscriptionEndDate(endDate);
            subscription.setNextBillingDate(endDate);
            log.info("Updated subscription end date to: {}", endDate);
        }
    }

    private String getOrCreateStripeCustomer(Doctor doctor, Subscription subscription) throws StripeException {
        log.info("=== GET OR CREATE STRIPE CUSTOMER === Doctor ID: {}", doctor.getId());

        if (subscription.getStripeCustomerId() != null) {
            log.info("Existing Stripe customer found: {}", subscription.getStripeCustomerId());
            return subscription.getStripeCustomerId();
        }

        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(doctor.getEmail())
                .setName(doctor.getName())
                .putMetadata("doctor_id", doctor.getId().toString())
                .build();

        Customer customer = Customer.create(params);
        subscription.setStripeCustomerId(customer.getId());
        subscriptionRepository.save(subscription);

        log.info("Created NEW Stripe customer {} for doctor {}", customer.getId(), doctor.getId());
        return customer.getId();
    }

    private String buildStatusMessage(Subscription subscription, boolean isActive,
                                      boolean isTrialPeriod, boolean trialExpired, long daysRemaining) {
        if (trialExpired) {
            return "Your free trial has expired. Please subscribe to continue using IndigoRx.";
        }
        if (!isActive) {
            return "Your subscription has expired. Please renew to continue using IndigoRx.";
        }
        if (isTrialPeriod) {
            if (daysRemaining <= 7) {
                return String.format("Your free trial expires in %d days. Subscribe now to continue!", daysRemaining);
            }
            return String.format("You have %d days remaining in your free trial.", daysRemaining);
        }
        if (daysRemaining <= 30) {
            return String.format("Your subscription renews in %d days.", daysRemaining);
        }
        return "Your subscription is active.";
    }
}