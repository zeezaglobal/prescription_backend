package com.zeezaglobal.prescription.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Service.SubscriptionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only check subscription for prescription creation (POST /api/prescriptions)
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/prescriptions")) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Doctor) {
                Doctor doctor = (Doctor) authentication.getPrincipal();

                if (!subscriptionService.hasActiveSubscription(doctor.getId())) {
                    log.warn("Subscription access denied for doctor {} - prescription creation blocked",
                            doctor.getId());

                    response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED); // 402
                    response.setContentType("application/json");

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Your subscription has expired. Please subscribe to continue creating prescriptions.");
                    errorResponse.put("code", "SUBSCRIPTION_EXPIRED");
                    errorResponse.put("requiresSubscription", true);

                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}