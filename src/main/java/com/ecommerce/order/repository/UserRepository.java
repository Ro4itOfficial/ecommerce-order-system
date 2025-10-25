package com.ecommerce.order.repository;

import com.ecommerce.order.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Find user by username
    Optional<User> findByUsername(String username);

    // Find user by email
    Optional<User> findByEmail(String email);

    // Find user by username or email (for login)
    Optional<User> findByUsernameOrEmail(String username, String email);

    // Check if username exists
    boolean existsByUsername(String username);

    // Check if email exists
    boolean existsByEmail(String email);

    // Find user by email verification token
    Optional<User> findByEmailVerificationToken(String token);

    // Find user by password reset token
    Optional<User> findByPasswordResetToken(String token);

    // Find users with expired password reset tokens
    @Query("SELECT u FROM User u WHERE u.passwordResetTokenExpires < :now AND u.passwordResetToken IS NOT NULL")
    List<User> findUsersWithExpiredPasswordResetTokens(@Param("now") LocalDateTime now);

    // Update last login information
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.lastLoginIp = :ipAddress WHERE u.userId = :userId")
    int updateLastLogin(@Param("userId") UUID userId, 
                       @Param("loginTime") LocalDateTime loginTime, 
                       @Param("ipAddress") String ipAddress);

    // Increment failed login attempts
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username")
    int incrementFailedLoginAttempts(@Param("username") String username);

    // Reset failed login attempts
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = NULL WHERE u.username = :username")
    int resetFailedLoginAttempts(@Param("username") String username);

    // Lock user account
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = false, u.lockedUntil = :lockedUntil WHERE u.username = :username")
    int lockUserAccount(@Param("username") String username, @Param("lockedUntil") LocalDateTime lockedUntil);

    // Unlock expired locked accounts
    @Modifying
    @Query("UPDATE User u SET u.accountNonLocked = true, u.lockedUntil = NULL WHERE u.lockedUntil < :now")
    int unlockExpiredAccounts(@Param("now") LocalDateTime now);

    // Find users by role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") String role);

    // Find enabled users
    List<User> findByEnabled(boolean enabled);

    // Find users created between dates
    List<User> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Verify email
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true, u.emailVerificationToken = NULL WHERE u.userId = :userId")
    int verifyEmail(@Param("userId") UUID userId);

    // Update password
    @Modifying
    @Query("UPDATE User u SET u.password = :password, u.passwordChangedAt = :changedAt WHERE u.userId = :userId")
    int updatePassword(@Param("userId") UUID userId, 
                      @Param("password") String password, 
                      @Param("changedAt") LocalDateTime changedAt);

    // Delete inactive users
    @Modifying
    @Query("DELETE FROM User u WHERE u.enabled = false AND u.createdAt < :cutoffDate AND u.emailVerified = false")
    int deleteInactiveUnverifiedUsers(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Count users by role
    @Query("SELECT r, COUNT(u) FROM User u JOIN u.roles r GROUP BY r")
    List<Object[]> countUsersByRole();

    // Search users
    @Query("""
        SELECT u FROM User u 
        WHERE (:searchTerm IS NULL OR 
               LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR 
               LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR 
               LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR 
               LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
        """)
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}