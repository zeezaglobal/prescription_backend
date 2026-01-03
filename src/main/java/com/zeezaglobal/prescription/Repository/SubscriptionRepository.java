package com.zeezaglobal.prescription.Repository;



import com.zeezaglobal.prescription.Entities.Doctor;
import com.zeezaglobal.prescription.Entities.Subscription;
import com.zeezaglobal.prescription.Entities.Subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByDoctor(Doctor doctor);

    Optional<Subscription> findByDoctorId(Long doctorId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<Subscription> findByStripeCustomerId(String stripeCustomerId);

    List<Subscription> findByStatus(SubscriptionStatus status);

    // Find subscriptions expiring soon (for sending reminders)
    @Query("SELECT s FROM Subscription s WHERE s.status = :status AND s.trialEndDate BETWEEN :start AND :end")
    List<Subscription> findTrialsExpiringSoon(
            @Param("status") SubscriptionStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Find expired trials that need status update
    @Query("SELECT s FROM Subscription s WHERE s.status = 'TRIAL' AND s.trialEndDate < :now")
    List<Subscription> findExpiredTrials(@Param("now") LocalDateTime now);

    // Find subscriptions expiring soon
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.subscriptionEndDate BETWEEN :start AND :end")
    List<Subscription> findSubscriptionsExpiringSoon(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Check if doctor has active subscription
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s " +
            "WHERE s.doctor.id = :doctorId AND " +
            "((s.status = 'TRIAL' AND s.trialEndDate > :now) OR " +
            "(s.status = 'ACTIVE' AND s.subscriptionEndDate > :now))")
    boolean hasActiveSubscription(@Param("doctorId") Long doctorId, @Param("now") LocalDateTime now);
}
