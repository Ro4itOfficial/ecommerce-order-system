package com.ecommerce.order.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.cache.interceptor.SimpleKeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    @Value("${spring.cache.caffeine.spec:maximumSize=1000,expireAfterWrite=5m}")
    private String caffeineSpec;

    @Bean
    @Primary
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configure different caches with different settings
        cacheManager.registerCustomCache("orders",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats()
                .build()
        );
        
        cacheManager.registerCustomCache("order-items",
            Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );
        
        cacheManager.registerCustomCache("customers",
            Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .recordStats()
                .build()
        );
        
        cacheManager.registerCustomCache("user-sessions",
            Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats()
                .build()
        );
        
        cacheManager.registerCustomCache("order-statistics",
            Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        cacheManager.registerCustomCache("order-search",
            Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats()
                .build()
        );

        // Default cache configuration for any cache not explicitly configured
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .recordStats()
        );
        
        cacheManager.setAllowNullValues(false);
        
        log.info("Caffeine cache manager configured with custom cache settings");
        return cacheManager;
    }

    @Bean
    public Caffeine<Object, Object> caffeineConfig() {
        log.info("Creating Caffeine cache with spec: {}", caffeineSpec);
        return Caffeine.from(caffeineSpec);
    }

    @Override
    public CacheManager cacheManager() {
        return caffeineCacheManager();
    }

    @Override
    public CacheResolver cacheResolver() {
        return new SimpleCacheResolver(Objects.requireNonNull(cacheManager()));
    }

    @Override
    public KeyGenerator keyGenerator() {
        return new CustomKeyGenerator();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CustomCacheErrorHandler();
    }

    /**
     * Custom key generator for cache keys
     */
    public static class CustomKeyGenerator extends SimpleKeyGenerator {
        @Override
        public Object generate(Object target, java.lang.reflect.Method method, Object... params) {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(".");
            sb.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    sb.append(param.toString()).append(",");
                }
            }
            
            String key = sb.toString();
            log.debug("Generated cache key: {}", key);
            return key;
        }
    }

    /**
     * Custom error handler for cache operations
     */
    public static class CustomCacheErrorHandler extends SimpleCacheErrorHandler {
        @Override
        public void handleCacheGetError(RuntimeException exception, 
                                       org.springframework.cache.Cache cache, 
                                       Object key) {
            log.error("Cache get error - cache: {}, key: {}, error: {}", 
                cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCachePutError(RuntimeException exception, 
                                       org.springframework.cache.Cache cache, 
                                       Object key, 
                                       Object value) {
            log.error("Cache put error - cache: {}, key: {}, error: {}", 
                cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheEvictError(RuntimeException exception, 
                                        org.springframework.cache.Cache cache, 
                                        Object key) {
            log.error("Cache evict error - cache: {}, key: {}, error: {}", 
                cache.getName(), key, exception.getMessage());
        }

        @Override
        public void handleCacheClearError(RuntimeException exception, 
                                        org.springframework.cache.Cache cache) {
            log.error("Cache clear error - cache: {}, error: {}", 
                cache.getName(), exception.getMessage());
        }
    }
}