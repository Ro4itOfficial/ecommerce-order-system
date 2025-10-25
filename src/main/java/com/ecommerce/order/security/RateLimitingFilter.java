package com.ecommerce.order.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Value("${rate-limit.window-size-minutes:1}")
    private int windowSizeMinutes;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(request);
        String endpoint = request.getMethod() + ":" + request.getRequestURI();
        
        if (isRateLimited(clientId, endpoint)) {
            handleRateLimitExceeded(response, clientId, endpoint);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimited(String clientId, String endpoint) {
        String key = "rate_limit:" + clientId + ":" + endpoint;
        long currentTime = Instant.now().toEpochMilli();
        long windowStart = currentTime - (windowSizeMinutes * 60 * 1000L);

        try {
            // Remove old entries outside the sliding window
            redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);

            // Count requests in current window
            Long requestCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

            if (requestCount != null && requestCount < requestsPerMinute) {
                // Add current request to the window
                redisTemplate.opsForZSet().add(key, UUID.randomUUID().toString(), currentTime);
                
                // Set expiration for the key
                redisTemplate.expire(key, windowSizeMinutes, TimeUnit.MINUTES);
                
                log.debug("Rate limit check passed for client: {} endpoint: {} ({}/{})", 
                    clientId, endpoint, requestCount + 1, requestsPerMinute);
                return false;
            }

            log.warn("Rate limit exceeded for client: {} endpoint: {} ({})", 
                clientId, endpoint, requestCount);
            return true;

        } catch (Exception e) {
            log.error("Error checking rate limit for client: {} endpoint: {}", clientId, endpoint, e);
            // In case of Redis failure, allow the request
            return false;
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        // Fall back to IP address
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getHeader("X-Real-IP");
        }
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        // Handle multiple IPs in X-Forwarded-For
        if (clientIp != null && clientIp.contains(",")) {
            clientIp = clientIp.split(",")[0].trim();
        }

        return "ip:" + clientIp;
    }

    private void handleRateLimitExceeded(HttpServletResponse response, String clientId, String endpoint) 
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        
        // Add rate limit headers
        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Window", windowSizeMinutes + "m");
        response.setHeader("X-RateLimit-Retry-After", String.valueOf(windowSizeMinutes * 60));

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        errorResponse.put("error", "Too Many Requests");
        errorResponse.put("message", "Rate limit exceeded. Please try again later.");
        errorResponse.put("path", endpoint);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip rate limiting for health checks and static resources
        return path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/favicon.ico");
    }

    /**
     * Get current rate limit status for a client
     */
    public Map<String, Object> getRateLimitStatus(String clientId, String endpoint) {
        String key = "rate_limit:" + clientId + ":" + endpoint;
        long currentTime = Instant.now().toEpochMilli();
        long windowStart = currentTime - (windowSizeMinutes * 60 * 1000L);

        Long requestCount = redisTemplate.opsForZSet().count(key, windowStart, currentTime);

        Map<String, Object> status = new HashMap<>();
        status.put("limit", requestsPerMinute);
        status.put("remaining", Math.max(0, requestsPerMinute - (requestCount != null ? requestCount : 0)));
        status.put("reset", Instant.ofEpochMilli(currentTime + (windowSizeMinutes * 60 * 1000L)));
        status.put("window", windowSizeMinutes + " minutes");
        
        return status;
    }

    /**
     * Reset rate limit for a specific client (admin function)
     */
    public void resetRateLimit(String clientId) {
        Set<String> keys = redisTemplate.keys("rate_limit:" + clientId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Reset rate limit for client: {}", clientId);
        }
    }
}