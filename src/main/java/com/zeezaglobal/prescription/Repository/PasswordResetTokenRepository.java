package com.zeezaglobal.prescription.Repository;



import com.zeezaglobal.prescription.Entities.PasswordResetToken;
import com.zeezaglobal.prescription.Entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find valid token by token string
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.token = :token " +
            "AND t.used = false AND t.expiresAt > :now")
    Optional<PasswordResetToken> findValidToken(
            @Param("token") String token,
            @Param("now") LocalDateTime now
    );

    /**
     * Find by token string (regardless of validity)
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find latest valid token for email
     */
    @Query("SELECT t FROM PasswordResetToken t WHERE t.email = :email " +
            "AND t.used = false AND t.expiresAt > :now " +
            "ORDER BY t.createdAt DESC")
    Optional<PasswordResetToken> findLatestValidTokenByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now
    );

    /**
     * Invalidate all previous tokens for user
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidatePreviousTokens(@Param("user") User user);

    /**
     * Invalidate all previous tokens for email
     */
    @Modifying
    @Transactional
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.used = false")
    void invalidatePreviousTokensByEmail(@Param("email") String email);

    /**
     * Count tokens created in last hour (for rate limiting)
     */
    @Query("SELECT COUNT(t) FROM PasswordResetToken t WHERE t.email = :email " +
            "AND t.createdAt > :since")
    long countRecentTokenRequests(
            @Param("email") String email,
            @Param("since") LocalDateTime since
    );

    /**
     * Clean up expired tokens (for scheduled cleanup)
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :before")
    void deleteExpiredTokens(@Param("before") LocalDateTime before);
}
