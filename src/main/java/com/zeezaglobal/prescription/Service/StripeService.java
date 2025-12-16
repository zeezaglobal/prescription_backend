package com.zeezaglobal.prescription.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.RequestOptions;
import com.stripe.param.*;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Service
public class StripeService {
    @Autowired
    private DoctorRepository doctorRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${stripe.monthly-price-id}")
    private String monthlyPriceId;

    @Value("${stripe.yearly-price-id}")
    private String yearlyPriceId;

    public String createCustomer(String email) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(email)

                .build();
        Customer customer = Customer.create(params);
        return customer.getId();
    }
    public String createPaymentIntent(Long doctorId) throws StripeException {
        // Set the Stripe API key
        Stripe.apiKey = stripeSecretKey;

        // Choose the priceId based on the request (monthly or yearly)
        String priceId = yearlyPriceId;
        Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        String customerId = null;
        if (doctor.isPresent()) {
            //customerId = doctor.get().getStripeUsername();
            // use customerId
        } else {
            // handle case where doctor is not found
        }
        // Get the amount for the selected price ID (this assumes you have a method to get the amount based on priceId)
        long amount = getPriceAmount(priceId);

        // Create a PaymentIntent using the priceId
        PaymentIntent paymentIntent = PaymentIntent.create(
                PaymentIntentCreateParams.builder()
                        .setCustomer(customerId)  // Set the customer ID
                        .setAmount(amount)         // Set the amount (in cents)
                        .setCurrency("inr")        // Set your currency here
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)  // Enable automatic payment methods
                                        .setAllowRedirects(PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)  // Disable redirects
                                        .build()
                        )

                        .build()
        );

        // Return the client secret of the PaymentIntent
        return paymentIntent.getClientSecret();
    }

    // This method retrieves the price amount based on the priceId
// You may need to replace this method with an actual implementation to retrieve price amounts.
    private long getPriceAmount(String priceId) throws StripeException {
        Price price = Price.retrieve(priceId);  // Retrieve the price object using the priceId
        return price.getUnitAmount();           // Return the unit amount associated with the price
    }

    public void attachPaymentMethod(Map<String, String> payload) throws StripeException {
        // Set the Stripe API key
        Stripe.apiKey = stripeSecretKey;

        // Retrieve the payment method using the ID from the request payload
        PaymentMethod paymentMethod = PaymentMethod.retrieve(payload.get("paymentMethodId"));
        Customer customer = Customer.retrieve(payload.get("customerId"));
        // Attach the payment method to the customer
        paymentMethod.attach(PaymentMethodAttachParams.builder()
                .setCustomer(payload.get("customerId"))
                .build());
        CustomerUpdateParams params = CustomerUpdateParams.builder()
                .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                        .setDefaultPaymentMethod(payload.get("paymentMethodId"))
                        .build())
                .build();
        Customer updatedCustomer = customer.update(params);
    }
    public String createSubscription(String customerId, boolean isMonthly) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        String priceId = isMonthly ? monthlyPriceId : yearlyPriceId;

        SubscriptionCreateParams params = SubscriptionCreateParams.builder()
                .setCustomer(customerId)
                .addItem(SubscriptionCreateParams.Item.builder().setPrice(priceId).build())
                .build();

        Subscription subscription = Subscription.create(params);
        return subscription.getId();
    }


}
