package com.zeezaglobal.prescription.Repository;



import com.zeezaglobal.prescription.Entities.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /**
     * Find the latest valid OTP for an email and type
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.email = :email AND o.type = :type " +
            "AND o.used = false AND o.expiresAt > :now AND o.blocked = false " +
            "ORDER BY o.createdAt DESC")
    Optional<OtpVerification> findLatestValidOtp(
            @Param("email") String email,
            @Param("type") OtpVerification.OtpType type,
            @Param("now") LocalDateTime now
    );

    /**
     * Find OTP by email, code, and type (for verification)
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.email = :email AND o.otp = :otp " +
            "AND o.type = :type AND o.used = false AND o.expiresAt > :now AND o.blocked = false")
    Optional<OtpVerification> findByEmailAndOtpAndType(
            @Param("email") String email,
            @Param("otp") String otp,
            @Param("type") OtpVerification.OtpType type,
            @Param("now") LocalDateTime now
    );

    /**
     * Find all OTPs for an email (for history/debugging)
     */
    @Query("SELECT o FROM OtpVerification o WHERE o.email = :email AND o.type = :type " +
            "AND o.createdAt > :since ORDER BY o.createdAt DESC")
    List<OtpVerification> findRecentOtps(
            @Param("email") String email,
            @Param("type") OtpVerification.OtpType type,
            @Param("since") LocalDateTime since
    );

    /**
     * Count OTP requests in time period (for rate limiting)
     */
    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.email = :email AND o.createdAt > :since")
    long countRecentOtpRequests(
            @Param("email") String email,
            @Param("since") LocalDateTime since
    );

    /**
     * Check if email is blocked due to too many failed attempts
     */
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM OtpVerification o " +
            "WHERE o.email = :email AND o.blocked = true AND o.blockedUntil > :now")
    boolean isEmailBlocked(
            @Param("email") String email,
            @Param("now") LocalDateTime now
    );

    /**
     * Invalidate all previous OTPs for an email and type
     */
    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.used = true WHERE o.email = :email " +
            "AND o.type = :type AND o.used = false")
    void invalidatePreviousOtps(
            @Param("email") String email,
            @Param("type") OtpVerification.OtpType type
    );

    /**
     * Clean up expired OTPs (for scheduled cleanup task)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OtpVerification o WHERE o.expiresAt < :before")
    void deleteExpiredOtps(@Param("before") LocalDateTime before);

    /**
     * Unblock email after block period expires
     */
    @Modifying
    @Transactional
    @Query("UPDATE OtpVerification o SET o.blocked = false, o.blockedUntil = null " +
            "WHERE o.email = :email AND o.blocked = true AND o.blockedUntil < :now")
    void unblockExpiredBlocks(
            @Param("email") String email,
            @Param("now") LocalDateTime now
    );
}
