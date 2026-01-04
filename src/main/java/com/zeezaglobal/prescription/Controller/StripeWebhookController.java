package com.zeezaglobal.prescription.Controller;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.zeezaglobal.prescription.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final SubscriptionService subscriptionService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

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
            // Still return 200 to prevent Stripe from retrying indefinitely
        }

        return ResponseEntity.ok("Webhook processed");
    }

    private void handleCheckoutSessionCompleted(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Session session = (Session) deserializer.getObject().get();
            try {
                subscriptionService.handleCheckoutSessionCompleted(session);
                log.info("Processed checkout.session.completed for session: {}", session.getId());
            } catch (Exception e) {
                log.error("Error handling checkout session completed: {}", e.getMessage(), e);
            }
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
            subscriptionService.handleSubscriptionDeleted(stripeSubscription);
            log.info("Processed subscription deletion for: {}", stripeSubscription.getId());
        }
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Invoice invoice = (Invoice) deserializer.getObject().get();
            subscriptionService.handleInvoicePaymentSucceeded(invoice);
            log.info("Processed invoice payment succeeded for: {}", invoice.getId());
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Invoice invoice = (Invoice) deserializer.getObject().get();
            subscriptionService.handleInvoicePaymentFailed(invoice);
            log.info("Processed invoice payment failed for: {}", invoice.getId());
        }
    }

    private void handleTrialWillEnd(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isPresent()) {
            Subscription stripeSubscription = (Subscription) deserializer.getObject().get();
            subscriptionService.handleTrialWillEnd(stripeSubscription);
            log.info("Processed trial will end for: {}", stripeSubscription.getId());
        }
    }
}