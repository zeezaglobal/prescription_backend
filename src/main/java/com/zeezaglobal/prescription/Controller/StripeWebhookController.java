package com.zeezaglobal.prescription.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import com.zeezaglobal.prescription.Service.SubscriptionService;
import com.zeezaglobal.prescription.Service.PostmarkEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;
    private final PostmarkEmailService emailService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // Simple in-memory idempotency check (consider using Redis/DB for production)
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing webhook event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        // Idempotency check - prevent duplicate processing
        String eventId = event.getId();
        if (processedEventIds.contains(eventId)) {
            log.info("Event {} already processed, skipping", eventId);
            return ResponseEntity.ok("Event already processed");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook event: {} (ID: {})", eventType, eventId);

        try {
            switch (eventType) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;

                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;

                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;

                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;

                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;

                case "customer.subscription.trial_will_end":
                    handleTrialWillEnd(event);
                    break;

                case "customer.subscription.paused":
                    handleSubscriptionPaused(event);
                    break;

                case "customer.subscription.resumed":
                    handleSubscriptionResumed(event);
                    break;

                default:
                    log.info("Unhandled event type: {}", eventType);
            }

            // Mark event as processed
            processedEventIds.add(eventId);

            // Clean up old event IDs (keep last 10000)
            if (processedEventIds.size() > 10000) {
                processedEventIds.clear();
            }

        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", eventType, e.getMessage(), e);
            // Still return 200 to prevent Stripe from retrying indefinitely
        }

        return ResponseEntity.ok("Webhook processed");
    }

    /**
     * Extracts the actual object JSON from the event data.
     */
    private String extractObjectJson(Event event) {
        String rawJson = event.getData().toJson();
        try {
            JsonObject wrapper = JsonParser.parseString(rawJson).getAsJsonObject();
            if (wrapper.has("object")) {
                return wrapper.get("object").toString();
            }
            return rawJson;
        } catch (Exception e) {
            log.warn("Could not parse JSON wrapper, using raw JSON: {}", e.getMessage());
            return rawJson;
        }
    }

    /**
     * Handle successful checkout - new subscription created via checkout
     */
    private void handleCheckoutSessionCompleted(Event event) {
        log.info("=== WEBHOOK: handleCheckoutSessionCompleted START ===");

        Session session = deserializeObject(event, Session.class);
        if (session == null) return;

        try {
            subscriptionService.handleCheckoutSessionCompleted(session);

            // Send subscription confirmation email
            String customerEmail = session.getCustomerEmail();
            if (customerEmail != null) {
                String customerName = session.getCustomerDetails() != null
                        ? session.getCustomerDetails().getName()
                        : null;
                emailService.sendSubscriptionConfirmationEmail(customerEmail, customerName);
            }

            log.info("=== WEBHOOK: handleCheckoutSessionCompleted END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling checkout session completed: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle new subscription created
     */
    private void handleSubscriptionCreated(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionCreated START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            subscriptionService.handleSubscriptionCreated(stripeSubscription);
            log.info("=== WEBHOOK: handleSubscriptionCreated END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling subscription created: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle subscription updates - plan changes, cancellation scheduled, status changes
     */
    private void handleSubscriptionUpdated(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionUpdated START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            String status = stripeSubscription.getStatus();
            Boolean cancelAtPeriodEnd = stripeSubscription.getCancelAtPeriodEnd();

            log.info("Subscription {} status: {}, cancelAtPeriodEnd: {}",
                    stripeSubscription.getId(), status, cancelAtPeriodEnd);

            // Check if cancellation was scheduled
            if (Boolean.TRUE.equals(cancelAtPeriodEnd)) {
                log.info("Subscription {} scheduled for cancellation at period end", stripeSubscription.getId());
                subscriptionService.handleSubscriptionCancellationScheduled(stripeSubscription);

                // Send cancellation scheduled email
                sendCancellationScheduledEmail(stripeSubscription);
            }

            // Handle status changes
            switch (status) {
                case "active":
                    subscriptionService.handleSubscriptionActivated(stripeSubscription);
                    break;
                case "past_due":
                    subscriptionService.handleSubscriptionPastDue(stripeSubscription);
                    sendPaymentPastDueEmail(stripeSubscription);
                    break;
                case "unpaid":
                    subscriptionService.handleSubscriptionUnpaid(stripeSubscription);
                    sendSubscriptionSuspendedEmail(stripeSubscription);
                    break;
                case "canceled":
                    subscriptionService.handleSubscriptionCanceled(stripeSubscription);
                    break;
                default:
                    subscriptionService.handleSubscriptionUpdated(stripeSubscription);
            }

            log.info("=== WEBHOOK: handleSubscriptionUpdated END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling subscription updated: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle subscription deleted - subscription has ended
     */
    private void handleSubscriptionDeleted(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionDeleted START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            subscriptionService.handleSubscriptionDeleted(stripeSubscription);

            // Send subscription ended email
            sendSubscriptionEndedEmail(stripeSubscription);

            log.info("=== WEBHOOK: handleSubscriptionDeleted END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling subscription deleted: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle successful invoice payment - includes renewals
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        log.info("=== WEBHOOK: handleInvoicePaymentSucceeded START ===");

        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null) return;

        try {
            String billingReason = invoice.getBillingReason();
            log.info("Invoice {} payment succeeded, billing reason: {}", invoice.getId(), billingReason);

            subscriptionService.handleInvoicePaymentSucceeded(invoice);

            // Send appropriate email based on billing reason
            if ("subscription_cycle".equals(billingReason)) {
                // This is a renewal payment
                sendRenewalSuccessEmail(invoice);
            } else if ("subscription_create".equals(billingReason)) {
                // Initial subscription payment - already handled in checkout.session.completed
                log.info("Initial subscription payment, email already sent via checkout");
            }

            log.info("=== WEBHOOK: handleInvoicePaymentSucceeded END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling invoice payment succeeded: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle failed invoice payment
     */
    private void handleInvoicePaymentFailed(Event event) {
        log.info("=== WEBHOOK: handleInvoicePaymentFailed START ===");

        Invoice invoice = deserializeObject(event, Invoice.class);
        if (invoice == null) return;

        try {
            int attemptCount = invoice.getAttemptCount() != null ? invoice.getAttemptCount().intValue() : 1;
            boolean nextPaymentAttempt = invoice.getNextPaymentAttempt() != null;

            log.info("Invoice {} payment failed, attempt: {}, will retry: {}",
                    invoice.getId(), attemptCount, nextPaymentAttempt);

            subscriptionService.handleInvoicePaymentFailed(invoice);

            // Send payment failed email with appropriate urgency
            sendPaymentFailedEmail(invoice, attemptCount, nextPaymentAttempt);

            log.info("=== WEBHOOK: handleInvoicePaymentFailed END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling invoice payment failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle trial ending soon notification (3 days before)
     */
    private void handleTrialWillEnd(Event event) {
        log.info("=== WEBHOOK: handleTrialWillEnd START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            subscriptionService.handleTrialWillEnd(stripeSubscription);

            // Send trial ending soon email
            sendTrialEndingEmail(stripeSubscription);

            log.info("=== WEBHOOK: handleTrialWillEnd END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling trial will end: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle subscription paused
     */
    private void handleSubscriptionPaused(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionPaused START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            subscriptionService.handleSubscriptionPaused(stripeSubscription);
            sendSubscriptionPausedEmail(stripeSubscription);
            log.info("=== WEBHOOK: handleSubscriptionPaused END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling subscription paused: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle subscription resumed
     */
    private void handleSubscriptionResumed(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionResumed START ===");

        Subscription stripeSubscription = deserializeObject(event, Subscription.class);
        if (stripeSubscription == null) return;

        try {
            subscriptionService.handleSubscriptionResumed(stripeSubscription);
            sendSubscriptionResumedEmail(stripeSubscription);
            log.info("=== WEBHOOK: handleSubscriptionResumed END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling subscription resumed: {}", e.getMessage(), e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generic deserializer for Stripe objects
     */
    private <T> T deserializeObject(Event event, Class<T> clazz) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            @SuppressWarnings("unchecked")
            T obj = (T) deserializer.getObject().get();
            log.info("Deserialized {} using getObject()", clazz.getSimpleName());
            return obj;
        }

        // Fallback: deserialize from raw JSON
        log.warn("getObject() returned empty, using raw JSON deserialization for {}", clazz.getSimpleName());
        try {
            String objectJson = extractObjectJson(event);
            T obj = ApiResource.GSON.fromJson(objectJson, clazz);
            if (obj == null) {
                log.error("{} is null after deserialization!", clazz.getSimpleName());
            }
            return obj;
        } catch (Exception e) {
            log.error("Failed to deserialize {}: {}", clazz.getSimpleName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get customer email from subscription
     */
    private String getCustomerEmail(Subscription subscription) {
        try {
            if (subscription.getCustomerObject() != null) {
                return subscription.getCustomerObject().getEmail();
            }
            // Fallback: fetch from our database using stripe customer ID
            return subscriptionService.getEmailByStripeCustomerId(subscription.getCustomer());
        } catch (Exception e) {
            log.warn("Could not get customer email: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get customer name from subscription
     */
    private String getCustomerName(Subscription subscription) {
        try {
            if (subscription.getCustomerObject() != null) {
                return subscription.getCustomerObject().getName();
            }
            return subscriptionService.getNameByStripeCustomerId(subscription.getCustomer());
        } catch (Exception e) {
            log.warn("Could not get customer name: {}", e.getMessage());
            return null;
        }
    }

    // ==================== EMAIL SENDING METHODS ====================

    private void sendCancellationScheduledEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            Long cancelAt = subscription.getCancelAt();
            String cancelDate = cancelAt != null
                    ? java.time.Instant.ofEpochSecond(cancelAt)
                    .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))
                    : "end of billing period";
            emailService.sendCancellationScheduledEmail(email, name, cancelDate);
        }
    }

    private void sendPaymentPastDueEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            emailService.sendPaymentPastDueEmail(email, name);
        }
    }

    private void sendSubscriptionSuspendedEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            emailService.sendSubscriptionSuspendedEmail(email, name);
        }
    }

    private void sendSubscriptionEndedEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            emailService.sendSubscriptionEndedEmail(email, name);
        }
    }

    private void sendRenewalSuccessEmail(Invoice invoice) {
        String email = invoice.getCustomerEmail();
        String name = invoice.getCustomerName();
        Long amountPaid = invoice.getAmountPaid();
        String amount = amountPaid != null ? "₹" + (amountPaid / 100) : "₹6,000";

        if (email != null) {
            emailService.sendRenewalSuccessEmail(email, name, amount);
        }
    }

    private void sendPaymentFailedEmail(Invoice invoice, int attemptCount, boolean willRetry) {
        String email = invoice.getCustomerEmail();
        String name = invoice.getCustomerName();

        if (email != null) {
            emailService.sendPaymentFailedEmail(email, name, attemptCount, willRetry);
        }
    }

    private void sendTrialEndingEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);

        if (email != null) {
            Long trialEnd = subscription.getTrialEnd();
            String endDate = trialEnd != null
                    ? java.time.Instant.ofEpochSecond(trialEnd)
                    .atZone(java.time.ZoneId.of("Asia/Kolkata"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy"))
                    : "soon";
            emailService.sendTrialEndingEmail(email, name, endDate);
        }
    }

    private void sendSubscriptionPausedEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            emailService.sendSubscriptionPausedEmail(email, name);
        }
    }

    private void sendSubscriptionResumedEmail(Subscription subscription) {
        String email = getCustomerEmail(subscription);
        String name = getCustomerName(subscription);
        if (email != null) {
            emailService.sendSubscriptionResumedEmail(email, name);
        }
    }
}