package com.ecommerce.order.service;

import com.ecommerce.order.model.dto.request.LoginRequest;
import com.ecommerce.order.model.dto.request.RegisterRequest;
import com.ecommerce.order.model.dto.response.JwtResponse;
import com.ecommerce.order.model.dto.response.MessageResponse;

public interface AuthService {
    
    /**
     * Register a new user
     */
    MessageResponse register(RegisterRequest request);
    
    /**
     * Authenticate user and generate JWT tokens
     */
    JwtResponse login(LoginRequest request, String ipAddress);
    
    /**
     * Refresh access token using refresh token
     */
    JwtResponse refreshToken(String refreshToken);
    
    /**
     * Logout user (invalidate tokens)
     */
    MessageResponse logout(String username);
    
    /**
     * Verify email with token
     */
    MessageResponse verifyEmail(String token);
    
    /**
     * Request password reset
     */
    MessageResponse requestPasswordReset(String email);
    
    /**
     * Reset password with token
     */
    MessageResponse resetPassword(String token, String newPassword);
    
    /**
     * Change password for authenticated user
     */
    MessageResponse changePassword(String username, String oldPassword, String newPassword);
}