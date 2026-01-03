package com.zeezaglobal.prescription.Aspect;

import com.zeezaglobal.prescription.Service.SubscriptionService;
import com.zeezaglobal.prescription.Utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;



import com.zeezaglobal.prescription.Exception.SubscriptionExpiredException;

@Aspect
@Component
@RequiredArgsConstructor
public class SubscriptionCheckAspect {

    private final SubscriptionService subscriptionService;
    private final JwtUtil jwtUtil;

    @Before("@annotation(com.zeezaglobal.prescription.Annotation.RequiresActiveSubscription)")
    public void checkSubscription(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof String && ((String) arg).startsWith("Bearer ")) {
                String token = ((String) arg).substring(7);
                Long doctorId = jwtUtil.extractUserId(token);

                if (!subscriptionService.hasActiveSubscription(doctorId)) {
                    throw new SubscriptionExpiredException("Your subscription has expired. Please subscribe to continue.");
                }
                return;
            }
        }
    }
}