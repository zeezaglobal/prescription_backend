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
        Optional<Subscription> existing = subscriptionRepository.findByDoctor(doctor);

        if (existing.isPresent()) {
            Subscription sub = existing.get();
            // Don't allow restarting trial if they've already had one
            if (sub.getTrialStartDate() != null) {
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

        return subscriptionRepository.save(subscription);
    }

    public SubscriptionDTO.SubscriptionStatusResponse getSubscriptionStatus(Long doctorId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);

        if (subscriptionOpt.isEmpty()) {
            return SubscriptionDTO.SubscriptionStatusResponse.builder()
                    .hasActiveSubscription(false)
                    .status(null)
                    .message("No subscription found. Please contact support.")
                    .build();
        }

        Subscription subscription = subscriptionOpt.get();
        boolean isActive = subscription.isActive();
        boolean isTrialPeriod = subscription.getStatus() == Subscription.SubscriptionStatus.TRIAL;
        boolean trialExpired = subscription.isTrialExpired();

        long daysRemaining;
        LocalDateTime expiryDate;

        if (isTrialPeriod) {
            daysRemaining = subscription.getTrialDaysRemaining();
            expiryDate = subscription.getTrialEndDate();
        } else {
            daysRemaining = subscription.getSubscriptionDaysRemaining();
            expiryDate = subscription.getSubscriptionEndDate();
        }

        String message = buildStatusMessage(subscription, isActive, isTrialPeriod, trialExpired, daysRemaining);

        return SubscriptionDTO.SubscriptionStatusResponse.builder()
                .hasActiveSubscription(isActive)
                .status(subscription.getStatus())
                .plan(subscription.getPlan())
                .daysRemaining(daysRemaining)
                .isTrialPeriod(isTrialPeriod)
                .trialExpired(trialExpired)
                .expiryDate(expiryDate)
                .message(message)
                .build();
    }

    public SubscriptionDTO.SubscriptionResponse getSubscriptionDetails(Long doctorId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByDoctorId(doctorId);

        // Handle case where no subscription exists yet
        if (subscriptionOpt.isEmpty()) {
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
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new RuntimeException("Doctor not found"));

        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseGet(() -> initializeTrialForDoctor(doctor));

        String customerId = getOrCreateStripeCustomer(doctor, subscription);

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
        log.info("Created checkout session {} for doctor {}", session.getId(), doctorId);

        return SubscriptionDTO.CheckoutSessionResponse.builder()
                .sessionId(session.getId())
                .checkoutUrl(session.getUrl())
                .publishableKey(stripeConfig.getPublishableKey())
                .build();
    }

    public SubscriptionDTO.BillingPortalResponse createBillingPortalSession(Long doctorId, String returnUrl) throws StripeException {
        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getStripeCustomerId() == null) {
            throw new RuntimeException("No Stripe customer found. Please subscribe first.");
        }

        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(subscription.getStripeCustomerId())
                        .setReturnUrl(returnUrl)
                        .build();

        com.stripe.model.billingportal.Session session =
                com.stripe.model.billingportal.Session.create(params);

        return SubscriptionDTO.BillingPortalResponse.builder()
                .portalUrl(session.getUrl())
                .build();
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) throws StripeException {
        String doctorIdStr = session.getMetadata().get("doctor_id");
        if (doctorIdStr == null) {
            log.error("No doctor_id in checkout session metadata");
            return;
        }

        Long doctorId = Long.parseLong(doctorIdStr);
        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for doctor: " + doctorId));

        String stripeSubscriptionId = session.getSubscription();
        com.stripe.model.Subscription stripeSubscription =
                com.stripe.model.Subscription.retrieve(stripeSubscriptionId);

        subscription.setStripeSubscriptionId(stripeSubscriptionId);
        subscription.setStripeCustomerId(session.getCustomer());
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setPlan(Subscription.SubscriptionPlan.YEARLY);
        subscription.setSubscriptionStartDate(LocalDateTime.now());
        subscription.setSubscriptionEndDate(LocalDateTime.now().plusYears(1));
        subscription.setNextBillingDate(LocalDateTime.now().plusYears(1));
        subscription.setAmountPaid(stripeConfig.getYearlyAmountPaise());
        subscription.setLastPaymentDate(LocalDateTime.now());

        subscriptionRepository.save(subscription);
        log.info("Subscription activated for doctor {} with Stripe subscription {}", doctorId, stripeSubscriptionId);
    }

    @Transactional
    public void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        String subscriptionId = stripeSubscription.getId();
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for Stripe subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        String status = stripeSubscription.getStatus();

        switch (status) {
            case "active":
                subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
                break;
            case "past_due":
                subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
                break;
            case "canceled":
                subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
                subscription.setCancellationDate(LocalDateTime.now());
                break;
            case "unpaid":
                subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
                break;
        }

        Long currentPeriodEnd = stripeSubscription.getCurrentPeriodEnd();
        if (currentPeriodEnd != null) {
            subscription.setSubscriptionEndDate(
                    LocalDateTime.ofEpochSecond(currentPeriodEnd, 0, java.time.ZoneOffset.UTC)
            );
            subscription.setNextBillingDate(
                    LocalDateTime.ofEpochSecond(currentPeriodEnd, 0, java.time.ZoneOffset.UTC)
            );
        }

        subscriptionRepository.save(subscription);
        log.info("Updated subscription {} to status {}", subscriptionId, status);
    }

    @Transactional
    public void handleInvoicePaymentSucceeded(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) return;

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        if (subscriptionOpt.isEmpty()) {
            log.warn("No local subscription found for invoice subscription: {}", subscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.ACTIVE);
        subscription.setLastPaymentDate(LocalDateTime.now());
        subscription.setAmountPaid(invoice.getAmountPaid().intValue());

        subscriptionRepository.save(subscription);
        log.info("Payment succeeded for subscription {}", subscriptionId);
    }

    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        String subscriptionId = invoice.getSubscription();
        if (subscriptionId == null) return;

        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
        if (subscriptionOpt.isEmpty()) return;

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(Subscription.SubscriptionStatus.PAST_DUE);
        subscriptionRepository.save(subscription);
        log.warn("Payment failed for subscription {}", subscriptionId);
    }

    @Transactional
    public void cancelSubscription(Long doctorId, SubscriptionDTO.CancelSubscriptionRequest request) throws StripeException {
        Subscription subscription = subscriptionRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getStripeSubscriptionId() != null) {
            com.stripe.model.Subscription stripeSubscription =
                    com.stripe.model.Subscription.retrieve(subscription.getStripeSubscriptionId());

            if (request.isImmediate()) {
                stripeSubscription.cancel();
                subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build();
                stripeSubscription.update(params);
            }
        }

        subscription.setCancellationDate(LocalDateTime.now());
        subscription.setCancellationReason(request.getReason());

        if (request.isImmediate()) {
            subscription.setStatus(Subscription.SubscriptionStatus.CANCELLED);
        }

        subscriptionRepository.save(subscription);
        log.info("Subscription cancelled for doctor {}", doctorId);
    }

    public boolean hasActiveSubscription(Long doctorId) {
        return subscriptionRepository.hasActiveSubscription(doctorId, LocalDateTime.now());
    }

    @Transactional
    public void updateExpiredTrials() {
        List<Subscription> expiredTrials = subscriptionRepository.findExpiredTrials(LocalDateTime.now());
        for (Subscription subscription : expiredTrials) {
            subscription.setStatus(Subscription.SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(subscription);
            log.info("Trial expired for doctor {}", subscription.getDoctor().getId());
        }
    }

    public List<Subscription> getTrialsExpiringSoon(int daysBeforeExpiry) {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().plusDays(daysBeforeExpiry);
        return subscriptionRepository.findTrialsExpiringSoon(Subscription.SubscriptionStatus.TRIAL, start, end);
    }

    private String getOrCreateStripeCustomer(Doctor doctor, Subscription subscription) throws StripeException {
        if (subscription.getStripeCustomerId() != null) {
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