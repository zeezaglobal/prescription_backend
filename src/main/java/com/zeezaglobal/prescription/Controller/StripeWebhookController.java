package com.zeezaglobal.prescription.Controller;


import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zeezaglobal.prescription.Config.StripeConfig;

import com.zeezaglobal.prescription.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;
    private final StripeConfig stripeConfig;

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            // Verify webhook signature if secret is configured
            if (stripeConfig.getWebhookSecret() != null && !stripeConfig.getWebhookSecret().isEmpty()) {
                event = Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
            } else {
                // For development without webhook secret
                event = Event.GSON.fromJson(payload, Event.class);
                log.warn("Webhook signature verification skipped - no webhook secret configured");
            }
        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
        } catch (Exception e) {
            log.error("Error parsing webhook event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload");
        }

        String eventType = event.getType();
        log.info("Received Stripe webhook event: {}", eventType);

        try {
            switch (eventType) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;

                case "customer.subscription.created":
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

                default:
                    log.info("Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing webhook event {}: {}", eventType, e.getMessage(), e);
            // Still return 200 to prevent Stripe from retrying
            // The error is logged for investigation
        }

        return ResponseEntity.ok("Webhook processed");
    }

    private void handleCheckoutSessionCompleted(Event event) throws Exception {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Session session = (Session) deserializer.getObject().get();
            subscriptionService.handleCheckoutSessionCompleted(session);
            log.info("Processed checkout.session.completed for session: {}", session.getId());
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Subscription stripeSubscription = (Subscription) deserializer.getObject().get();
            subscriptionService.handleSubscriptionUpdated(stripeSubscription);
            log.info("Processed subscription update for: {}", stripeSubscription.getId());
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Subscription stripeSubscription = (Subscription) deserializer.getObject().get();
            subscriptionService.handleSubscriptionUpdated(stripeSubscription);
            log.info("Processed subscription deletion for: {}", stripeSubscription.getId());
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Invoice invoice = (Invoice) deserializer.getObject().get();
            subscriptionService.handleInvoicePaymentSucceeded(invoice);
            log.info("Processed successful payment for invoice: {}", invoice.getId());
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Invoice invoice = (Invoice) deserializer.getObject().get();
            subscriptionService.handleInvoicePaymentFailed(invoice);
            log.warn("Processed failed payment for invoice: {}", invoice.getId());
        }
    }

    private void handleTrialWillEnd(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Subscription stripeSubscription = (Subscription) deserializer.getObject().get();
            // This event fires 3 days before trial ends
            // You could send a reminder email here
            log.info("Trial will end soon for subscription: {}", stripeSubscription.getId());
        }
    }
}