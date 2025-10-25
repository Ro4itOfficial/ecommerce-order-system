package com.ecommerce.order.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CircuitBreakerConfig {

    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                log.info("CircuitBreaker {} added", circuitBreaker.getName());
                
                circuitBreaker.getEventPublisher()
                    .onSuccess(event -> log.debug("CircuitBreaker {} succeeded", circuitBreaker.getName()))
                    .onError(event -> log.error("CircuitBreaker {} error: {}", circuitBreaker.getName(), event.getThrowable().getMessage()))
                    .onStateTransition(event -> log.warn("CircuitBreaker {} state transition from {} to {}", 
                        circuitBreaker.getName(), event.getStateTransition().getFromState(), event.getStateTransition().getToState()))
                    .onCallNotPermitted(event -> log.warn("CircuitBreaker {} call not permitted", circuitBreaker.getName()))
                    .onIgnoredError(event -> log.debug("CircuitBreaker {} ignored error: {}", circuitBreaker.getName(), event.getThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemovedEvent) {
                log.info("CircuitBreaker {} removed", entryRemovedEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("CircuitBreaker {} replaced", entryReplacedEvent.getOldEntry().getName());
            }
        };
    }

    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                log.info("Retry {} added", retry.getName());
                
                retry.getEventPublisher()
                    .onRetry(event -> log.debug("Retry {} - attempt {}", retry.getName(), event.getNumberOfRetryAttempts()))
                    .onSuccess(event -> log.debug("Retry {} succeeded after {} attempts", retry.getName(), event.getNumberOfRetryAttempts()))
                    .onError(event -> log.error("Retry {} failed after {} attempts: {}", 
                        retry.getName(), event.getNumberOfRetryAttempts(), event.getLastThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemovedEvent) {
                log.info("Retry {} removed", entryRemovedEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                log.info("Retry {} replaced", entryReplacedEvent.getOldEntry().getName());
            }
        };
    }
}