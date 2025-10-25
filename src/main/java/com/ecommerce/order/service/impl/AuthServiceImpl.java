package com.ecommerce.order.service.impl;

import com.ecommerce.order.exception.UserAlreadyExistsException;
import com.ecommerce.order.exception.InvalidCredentialsException;
import com.ecommerce.order.model.dto.request.LoginRequest;
import com.ecommerce.order.model.dto.request.RegisterRequest;
import com.ecommerce.order.model.dto.response.JwtResponse;
import com.ecommerce.order.model.dto.response.MessageResponse;
import com.ecommerce.order.model.entity.User;
import com.ecommerce.order.repository.UserRepository;
import com.ecommerce.order.security.JwtTokenProvider;
import com.ecommerce.order.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.getUsername());
        
        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username already exists: " + request.getUsername());
        }
        
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + request.getEmail());
        }
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .roles(new HashSet<>(Collections.singletonList("USER")))
                .enabled(true)
                .emailVerified(false)
                .emailVerificationToken(UUID.randomUUID().toString())
                .passwordChangedAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(user);
        
        // TODO: Send email verification
        // emailService.sendVerificationEmail(savedUser);
        
        log.info("User registered successfully: {}", savedUser.getUsername());
        
        return MessageResponse.success("User registered successfully. Please verify your email.");
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request, String ipAddress) {
        log.info("Login attempt for user: {}", request.getUsername());
        
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            User user = (User) authentication.getPrincipal();
            
            // Update login information
            user.recordSuccessfulLogin(ipAddress);
            userRepository.save(user);
            
            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(user);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);
            
            // Store refresh token in Redis with expiration
            String refreshTokenKey = "refresh_token:" + user.getUsername();
            redisTemplate.opsForValue().set(
                    refreshTokenKey, 
                    refreshToken, 
                    24, 
                    TimeUnit.HOURS
            );
            
            // Cache user session
            cacheUserSession(user.getUsername(), accessToken);
            
            log.info("User logged in successfully: {}", user.getUsername());
            
            return JwtResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(3600L)
                    .refreshExpiresIn(86400L)
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .roles(new ArrayList<>(user.getRoles()))
                    .issuedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusSeconds(3600))
                    .refreshExpiresAt(LocalDateTime.now().plusSeconds(86400))
                    .build();
                    
        } catch (BadCredentialsException e) {
            // Handle failed login
            Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.recordFailedLogin();
                userRepository.save(user);
            }
            
            log.error("Login failed for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    @Override
    public JwtResponse refreshToken(String refreshToken) {
        log.info("Refreshing access token");
        
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid refresh token");
        }
        
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        
        // Verify refresh token exists in Redis
        String storedToken = redisTemplate.opsForValue().get("refresh_token:" + username);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token not found or expired");
        }
        
        // Load user and generate new access token
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        
        // Update cached session
        cacheUserSession(username, newAccessToken);
        
        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Keep the same refresh token
                .tokenType("Bearer")
                .expiresIn(3600L)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(new ArrayList<>(user.getRoles()))
                .issuedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(3600))
                .build();
    }

    @Override
    @CacheEvict(value = "user-sessions", key = "#username")
    public MessageResponse logout(String username) {
        log.info("Logging out user: {}", username);
        
        // Remove refresh token from Redis
        redisTemplate.delete("refresh_token:" + username);
        
        // Clear security context
        SecurityContextHolder.clearContext();
        
        log.info("User logged out successfully: {}", username);
        
        return MessageResponse.success("Logged out successfully");
    }

    @Override
    @Transactional
    public MessageResponse verifyEmail(String token) {
        log.info("Verifying email with token");
        
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid verification token"));
        
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
        
        log.info("Email verified for user: {}", user.getUsername());
        
        return MessageResponse.success("Email verified successfully");
    }

    @Override
    @Transactional
    public MessageResponse requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Email not found"));
        
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpires(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        
        // TODO: Send password reset email
        // emailService.sendPasswordResetEmail(user);
        
        log.info("Password reset token generated for user: {}", user.getUsername());
        
        return MessageResponse.success("Password reset instructions sent to your email");
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(String token, String newPassword) {
        log.info("Resetting password with token");
        
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid reset token"));
        
        if (user.getPasswordResetTokenExpires().isBefore(LocalDateTime.now())) {
            throw new InvalidCredentialsException("Reset token has expired");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpires(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Invalidate all existing sessions
        redisTemplate.delete("refresh_token:" + user.getUsername());
        clearUserSession(user.getUsername());
        
        log.info("Password reset successfully for user: {}", user.getUsername());
        
        return MessageResponse.success("Password reset successfully");
    }

    @Override
    @Transactional
    public MessageResponse changePassword(String username, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Invalidate all existing sessions
        redisTemplate.delete("refresh_token:" + username);
        clearUserSession(username);
        
        log.info("Password changed successfully for user: {}", username);
        
        return MessageResponse.success("Password changed successfully. Please login again.");
    }
    
    @CachePut(value = "user-sessions", key = "#username")
    private String cacheUserSession(String username, String token) {
        return token;
    }
    
    @CacheEvict(value = "user-sessions", key = "#username")
    private void clearUserSession(String username) {
        log.debug("Clearing session cache for user: {}", username);
    }
}