package com.zeezaglobal.prescription.Controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.model.checkout.Session;
import com.stripe.net.ApiResource;
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

    /**
     * Extracts the actual object JSON from the event data.
     * The raw JSON has structure: {"previous_attributes": ..., "object": {...}}
     * We need just the "object" part for deserialization.
     */
    private String extractObjectJson(Event event) {
        String rawJson = event.getData().toJson();
        try {
            JsonObject wrapper = JsonParser.parseString(rawJson).getAsJsonObject();
            if (wrapper.has("object")) {
                return wrapper.get("object").toString();
            }
            // If no wrapper, return as-is
            return rawJson;
        } catch (Exception e) {
            log.warn("Could not parse JSON wrapper, using raw JSON: {}", e.getMessage());
            return rawJson;
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        log.info("=== WEBHOOK: handleCheckoutSessionCompleted START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Session session = null;

        if (deserializer.getObject().isPresent()) {
            session = (Session) deserializer.getObject().get();
            log.info("Deserialized session using getObject(): {}", session.getId());
        } else {
            // Fallback: deserialize from raw JSON (extract 'object' field)
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                log.info("Extracted object JSON (first 200 chars): {}",
                        objectJson.length() > 200 ? objectJson.substring(0, 200) : objectJson);
                session = ApiResource.GSON.fromJson(objectJson, Session.class);
                log.info("Deserialized session from raw JSON: {}", session != null ? session.getId() : "null");
            } catch (Exception e) {
                log.error("Failed to deserialize session from raw JSON: {}", e.getMessage(), e);
                return;
            }
        }

        if (session == null) {
            log.error("Session is null after deserialization attempts!");
            return;
        }

        try {
            subscriptionService.handleCheckoutSessionCompleted(session);
            log.info("=== WEBHOOK: handleCheckoutSessionCompleted END - SUCCESS ===");
        } catch (Exception e) {
            log.error("Error handling checkout session completed: {}", e.getMessage(), e);
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionUpdated START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Subscription stripeSubscription = null;

        if (deserializer.getObject().isPresent()) {
            stripeSubscription = (Subscription) deserializer.getObject().get();
            log.info("Deserialized subscription using getObject(): {}", stripeSubscription.getId());
        } else {
            // Fallback: deserialize from raw JSON (extract 'object' field)
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                log.info("Extracted object JSON (first 200 chars): {}",
                        objectJson.length() > 200 ? objectJson.substring(0, 200) : objectJson);
                stripeSubscription = ApiResource.GSON.fromJson(objectJson, Subscription.class);
                log.info("Deserialized subscription from raw JSON: {}",
                        stripeSubscription != null ? stripeSubscription.getId() : "null");
            } catch (Exception e) {
                log.error("Failed to deserialize subscription from raw JSON: {}", e.getMessage(), e);
                return;
            }
        }

        if (stripeSubscription == null) {
            log.error("Subscription is null after deserialization attempts!");
            return;
        }

        subscriptionService.handleSubscriptionUpdated(stripeSubscription);
        log.info("=== WEBHOOK: handleSubscriptionUpdated END - SUCCESS ===");
    }

    private void handleSubscriptionDeleted(Event event) {
        log.info("=== WEBHOOK: handleSubscriptionDeleted START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Subscription stripeSubscription = null;

        if (deserializer.getObject().isPresent()) {
            stripeSubscription = (Subscription) deserializer.getObject().get();
        } else {
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                stripeSubscription = ApiResource.GSON.fromJson(objectJson, Subscription.class);
            } catch (Exception e) {
                log.error("Failed to deserialize subscription: {}", e.getMessage(), e);
                return;
            }
        }

        if (stripeSubscription == null) {
            log.error("Subscription is null!");
            return;
        }

        subscriptionService.handleSubscriptionDeleted(stripeSubscription);
        log.info("=== WEBHOOK: handleSubscriptionDeleted END - SUCCESS ===");
    }

    private void handleInvoicePaymentSucceeded(Event event) {
        log.info("=== WEBHOOK: handleInvoicePaymentSucceeded START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Invoice invoice = null;

        if (deserializer.getObject().isPresent()) {
            invoice = (Invoice) deserializer.getObject().get();
            log.info("Deserialized invoice using getObject(): {}", invoice.getId());
        } else {
            // Fallback: deserialize from raw JSON (extract 'object' field)
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                log.info("Extracted object JSON (first 200 chars): {}",
                        objectJson.length() > 200 ? objectJson.substring(0, 200) : objectJson);
                invoice = ApiResource.GSON.fromJson(objectJson, Invoice.class);
                log.info("Deserialized invoice from raw JSON: {}", invoice != null ? invoice.getId() : "null");
            } catch (Exception e) {
                log.error("Failed to deserialize invoice from raw JSON: {}", e.getMessage(), e);
                return;
            }
        }

        if (invoice == null) {
            log.error("Invoice is null after deserialization attempts!");
            return;
        }

        subscriptionService.handleInvoicePaymentSucceeded(invoice);
        log.info("=== WEBHOOK: handleInvoicePaymentSucceeded END - SUCCESS ===");
    }

    private void handleInvoicePaymentFailed(Event event) {
        log.info("=== WEBHOOK: handleInvoicePaymentFailed START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Invoice invoice = null;

        if (deserializer.getObject().isPresent()) {
            invoice = (Invoice) deserializer.getObject().get();
        } else {
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                invoice = ApiResource.GSON.fromJson(objectJson, Invoice.class);
            } catch (Exception e) {
                log.error("Failed to deserialize invoice: {}", e.getMessage(), e);
                return;
            }
        }

        if (invoice == null) {
            log.error("Invoice is null!");
            return;
        }

        subscriptionService.handleInvoicePaymentFailed(invoice);
        log.info("=== WEBHOOK: handleInvoicePaymentFailed END - SUCCESS ===");
    }

    private void handleTrialWillEnd(Event event) {
        log.info("=== WEBHOOK: handleTrialWillEnd START ===");

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Subscription stripeSubscription = null;

        if (deserializer.getObject().isPresent()) {
            stripeSubscription = (Subscription) deserializer.getObject().get();
        } else {
            log.warn("getObject() returned empty, using raw JSON deserialization");
            try {
                String objectJson = extractObjectJson(event);
                stripeSubscription = ApiResource.GSON.fromJson(objectJson, Subscription.class);
            } catch (Exception e) {
                log.error("Failed to deserialize subscription: {}", e.getMessage(), e);
                return;
            }
        }

        if (stripeSubscription == null) {
            log.error("Subscription is null!");
            return;
        }

        subscriptionService.handleTrialWillEnd(stripeSubscription);
        log.info("=== WEBHOOK: handleTrialWillEnd END - SUCCESS ===");
    }
}