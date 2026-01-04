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

        // Get subscription end date from Stripe
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
        log.info("AFTER UPDATE - SubStart: {}, SubEnd: {}, NextBilling: {}",
                saved.getSubscriptionStartDate(), saved.getSubscriptionEndDate(), saved.getNextBillingDate());

        log.info("========================================");
        log.info("=== CHECKOUT SESSION COMPLETED END ===");
        log.info("=== STATUS SET TO: ACTIVE ===");
        log.info("========================================");
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
        log.info("Stripe Metadata: {}", stripeSubscription.getMetadata());

        // Try to find by subscription ID first
        log.info("Attempting to find subscription by Stripe Subscription ID...");
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        log.info("Found by subscription ID: {}", subscriptionOpt.isPresent());

        // Fallback: find by customer ID
        if (subscriptionOpt.isEmpty() && customerId != null) {
            log.info("Attempting to find subscription by Stripe Customer ID...");
            subscriptionOpt = subscriptionRepository.findByStripeCustomerId(customerId);
            log.info("Found by customer ID: {}", subscriptionOpt.isPresent());

            if (subscriptionOpt.isPresent()) {
                Subscription sub = subscriptionOpt.get();
                sub.setStripeSubscriptionId(subscriptionId);
                log.info("Linked Stripe subscription {} to existing customer {}", subscriptionId, customerId);
            }
        }

        // Fallback: try to find by metadata doctor_id
        if (subscriptionOpt.isEmpty()) {
            String doctorIdStr = stripeSubscription.getMetadata().get("doctor_id");
            log.info("Attempting to find subscription by doctor_id from metadata: {}", doctorIdStr);
            if (doctorIdStr != null) {
                Long doctorId = Long.parseLong(doctorIdStr);
                subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);
                log.info("Found by doctor ID: {}", subscriptionOpt.isPresent());

                if (subscriptionOpt.isPresent()) {
                    Subscription sub = subscriptionOpt.get();
                    sub.setStripeSubscriptionId(subscriptionId);
                    sub.setStripeCustomerId(customerId);
                    log.info("Linked Stripe subscription {} to doctor {}", subscriptionId, doctorId);
                }
            }
        }

        if (subscriptionOpt.isEmpty()) {
            log.warn("NO LOCAL SUBSCRIPTION FOUND! SubscriptionId: {}, CustomerId: {}", subscriptionId, customerId);
            log.info("=== SUBSCRIPTION UPDATED END (NO ACTION) ===");
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("BEFORE UPDATE - Local Subscription ID: {}, Status: {}, Plan: {}",
                subscription.getId(), subscription.getStatus(), subscription.getPlan());

        switch (stripeStatus) {
            case "active":
                log.info("Setting status to ACTIVE");
                subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);
                if (subscription.getSubscriptionStartDate() == null) {
                    subscription.setSubscriptionStartDate(LocalDateTime.now());
                    log.info("Set subscription start date to now");
                }
                break;
            case "trialing":
                log.info("Stripe status is 'trialing', current local status: {}", subscription.getStatus());
                if (subscription.getStatus() != Subscription.SubscriptionStatus.TRIAL) {
                    subscription.setStatus(Subscription.SubscriptionStatus.TRIAL);
                    log.info("Set status to TRIAL");
                } else {
                    log.info("Already in TRIAL status, no change");
                }
                break;
            case "past_due":
                log.info("Setting status to PAST_DUE");
                subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                break;
            case "canceled":
                log.info("Setting status to CANCELLED");
                subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                subscription.setCancellationDate(LocalDateTime.now());
                break;
            case "unpaid":
                log.info("Setting status to EXPIRED");
                subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
                break;
            default:
                log.info("Unhandled Stripe subscription status: {}", stripeStatus);
        }

        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        if (currentPeriodEnd != null) {
            LocalDateTime endDate = LocalDateTime.ofEpochSecond(currentPeriodEnd, 0, java.time.ZoneOffset.UTC);
            subscription.setSubscriptionEndDate(endDate);
            subscription.setNextBillingDate(endDate);
            log.info("Set subscription end date and next billing date to: {}", endDate);
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("AFTER UPDATE - Local Subscription ID: {}, Status: {}, Plan: {}",
                saved.getId(), saved.getStatus(), saved.getPlan());
        log.info("========================================");
        log.info("=== SUBSCRIPTION UPDATED END ===");
        log.info("========================================");
    }

    @Transactional
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== SUBSCRIPTION DELETED START ===");

        String subscriptionId = stripeSubscription.getId();
        String customerId = stripeSubscription.getCustomer();
        log.info("Stripe Subscription ID: {}, Customer ID: {}", subscriptionId, customerId);

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);

        if (subscriptionOpt.isEmpty() && customerId != null) {
            subscriptionOpt = subscriptionRepository.findByStripeCustomerId(customerId);
        }

        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for deleted Stripe subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        subscription.setCancellationDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        log.info("=== SUBSCRIPTION DELETED END === Marked as CANCELLED for doctor {}",
                subscription.getDoctor().getId());
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
        log.info("Currency: {}", invoice.getCurrency());

        if (subscriptionId == null) {
            log.info("Invoice has no subscription ID, skipping");
            return;
        }

        log.info("Attempting to find subscription by Stripe Subscription ID...");
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        log.info("Found by subscription ID: {}", subscriptionOpt.isPresent());

        if (subscriptionOpt.isEmpty() && customerId != null) {
            log.info("Attempting to find subscription by Stripe Customer ID...");
            subscriptionOpt = subscriptionRepository.findByStripeCustomerId(customerId);
            log.info("Found by customer ID: {}", subscriptionOpt.isPresent());
        }

        if (subscriptionOpt.isEmpty()) {
            log.warn("NO LOCAL SUBSCRIPTION FOUND! SubscriptionId: {}, CustomerId: {}", subscriptionId, customerId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("BEFORE UPDATE - Subscription ID: {}, Status: {}, Plan: {}, StripeSubId: {}",
                subscription.getId(), subscription.getStatus(), subscription.getPlan(),
                subscription.getStripeSubscriptionId());

        if (subscription.getStripeSubscriptionId() == null) {
            subscription.setStripeSubscriptionId(subscriptionId);
            log.info("Set Stripe subscription ID: {}", subscriptionId);
        }

        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);
        subscription.setLastPaymentDate(LocalDateTime.now());
        subscription.setAmountPaid(invoice.getAmountPaid() != null ? invoice.getAmountPaid().intValue() : 0);
        subscription.setCurrency(invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase() : "INR");

        if (subscription.getSubscriptionStartDate() == null) {
            subscription.setSubscriptionStartDate(LocalDateTime.now());
            log.info("Set subscription start date to now");
        }
        if (subscription.getSubscriptionEndDate() == null) {
            subscription.setSubscriptionEndDate(LocalDateTime.now().plusYears(1));
            log.info("Set subscription end date to 1 year from now");
        }

        Subscription saved = subscriptionRepository.save(subscription);
        log.info("AFTER UPDATE - Subscription ID: {}, Status: {}, Plan: {}",
                saved.getId(), saved.getStatus(), saved.getPlan());
        log.info("========================================");
        log.info("=== INVOICE PAYMENT SUCCEEDED END ===");
        log.info("=== STATUS SET TO: ACTIVE ===");
        log.info("========================================");
    }

    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        log.info("=== INVOICE PAYMENT FAILED START ===");

        String subscriptionId = invoice.getSubscription();
        String customerId = invoice.getCustomer();
        log.info("Subscription ID: {}, Customer ID: {}", subscriptionId, customerId);

        if (subscriptionId == null) {
            log.info("No subscription ID, skipping");
            return;
        }

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);

        if (subscriptionOpt.isEmpty() && customerId != null) {
            subscriptionOpt = subscriptionRepository.findByStripeCustomerId(customerId);
        }

        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for failed invoice. Subscription: {}, Customer: {}",
                    subscriptionId, customerId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
        log.warn("=== INVOICE PAYMENT FAILED END === Status updated to PAST_DUE for doctor {}",
                subscription.getDoctor().getId());
    }

    @Transactional
    public void handleTrialWillEnd(com.stripe.model.Subscription stripeSubscription) {
        log.info("=== TRIAL WILL END START ===");

        String subscriptionId = stripeSubscription.getId();
        String customerId = stripeSubscription.getCustomer();

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);

        if (subscriptionOpt.isEmpty() && customerId != null) {
            subscriptionOpt = subscriptionRepository.findByStripeCustomerId(customerId);
        }

        if (subscriptionOpt.isEmpty()) {
            log.info("Trial ending notification for unknown subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        log.info("=== TRIAL WILL END === Trial ending soon for doctor {} - subscription {}",
                subscription.getDoctor().getId(), subscriptionId);

        // TODO: Send email notification to doctor about trial ending
    }

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