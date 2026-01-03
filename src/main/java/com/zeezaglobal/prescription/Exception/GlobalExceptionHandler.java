package com.zeezaglobal.prescription.Exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SubscriptionExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionExpired(SubscriptionExpiredException ex) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED) // 402 status
                .body(Map.of(
                        "success", false,
                        "error", ex.getMessage(),
                        "code", "SUBSCRIPTION_EXPIRED",
                        "requiresSubscription", true
                ));
    }
}
