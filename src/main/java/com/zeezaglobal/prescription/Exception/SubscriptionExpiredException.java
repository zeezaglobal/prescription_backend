package com.zeezaglobal.prescription.Exception;

public class SubscriptionExpiredException extends RuntimeException {

    public SubscriptionExpiredException(String message) {
        super(message);
    }
}