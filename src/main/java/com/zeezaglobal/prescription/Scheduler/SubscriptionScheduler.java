package com.zeezaglobal.prescription.Scheduler;

import com.zeezaglobal.prescription.Entities.Subscription;

import com.zeezaglobal.prescription.Service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled tasks for subscription management
 *
 * Make sure to add @EnableScheduling to your main application class
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;
    // Inject your email service if you have one
    // private final EmailService emailService;

    /**
     * Check for expired trials daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void updateExpiredTrials() {
        log.info("Running scheduled task: Update expired trials");
        try {
            subscriptionService.updateExpiredTrials();
            log.info("Completed updating expired trials");
        } catch (Exception e) {
            log.error("Error updating expired trials: {}", e.getMessage(), e);
        }
    }

    /**
     * Send trial expiry reminders 7 days before expiration
     */
    @Scheduled(cron = "0 0 9 * * *") // Every day at 9 AM
    public void sendTrialExpiryReminders() {
        log.info("Running scheduled task: Send trial expiry reminders");
        try {
            List<Subscription> expiringTrials = subscriptionService.getTrialsExpiringSoon(7);

            for (Subscription subscription : expiringTrials) {
                try {
                    // Send reminder email
                    // emailService.sendTrialExpiryReminder(subscription.getDoctor());
                    log.info("Sent trial expiry reminder to doctor: {}",
                            subscription.getDoctor().getEmail());
                } catch (Exception e) {
                    log.error("Failed to send reminder to {}: {}",
                            subscription.getDoctor().getEmail(), e.getMessage());
                }
            }

            log.info("Sent {} trial expiry reminders", expiringTrials.size());
        } catch (Exception e) {
            log.error("Error sending trial expiry reminders: {}", e.getMessage(), e);
        }
    }

    /**
     * Send trial expiry urgent reminder 1 day before expiration
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendUrgentTrialReminders() {
        log.info("Running scheduled task: Send urgent trial reminders");
        try {
            List<Subscription> expiringTomorrow = subscriptionService.getTrialsExpiringSoon(1);

            for (Subscription subscription : expiringTomorrow) {
                try {
                    // Send urgent reminder email
                    // emailService.sendUrgentTrialExpiryReminder(subscription.getDoctor());
                    log.info("Sent urgent trial expiry reminder to doctor: {}",
                            subscription.getDoctor().getEmail());
                } catch (Exception e) {
                    log.error("Failed to send urgent reminder to {}: {}",
                            subscription.getDoctor().getEmail(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error sending urgent trial reminders: {}", e.getMessage(), e);
        }
    }
}