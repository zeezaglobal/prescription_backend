package com.zeezaglobal.prescription.Config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.secretKey}")
    private String secretKey;

    @Value("${stripe.publishableKey}")
    private String publishableKey;

    @Value("${stripe.monthly-price-id}")
    private String monthlyPriceId;

    @Value("${stripe.yearly-price-id}")
    private String yearlyPriceId;

    @Value("${stripe.webhook-secret:}")
    private String webhookSecret;

    // Trial period in days (3 months = ~90 days)
    private final int trialPeriodDays = 90;

    // Yearly subscription amount in paise (₹6000 = 600000 paise)
    private final int yearlyAmountPaise = 600000;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}