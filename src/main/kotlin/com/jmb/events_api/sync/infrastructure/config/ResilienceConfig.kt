package com.jmb.events_api.sync.infrastructure.config

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Resilience4j configuration that adds event listeners to instances
 * created by Spring Boot auto-configuration from application.yml
 */
@Configuration
class ResilienceConfig {

    private val logger = LoggerFactory.getLogger(ResilienceConfig::class.java)

    /**
     * Get circuit breaker instance and add event listeners for monitoring.
     * The circuit breaker itself is configured via application.yml
     */
    @Bean
    fun providerApiCircuitBreaker(circuitBreakerRegistry: CircuitBreakerRegistry): CircuitBreaker {
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("providerApi")

        // Add event listeners for monitoring and logging
        circuitBreaker.eventPublisher
            .onStateTransition { event ->
                logger.info("🔄 Provider API Circuit Breaker: ${event.stateTransition}")
            }
            .onFailureRateExceeded { event ->
                logger.warn("⚠️ Provider API failure rate exceeded: ${event.failureRate}%")
            }
            .onCallNotPermitted { event ->
                logger.warn("🚫 Provider API call blocked - Circuit Breaker is OPEN")
            }
            .onSuccess { event ->
                logger.debug("✅ Provider API call succeeded")
            }
            .onError { event ->
                logger.debug("❌ Provider API call failed: ${event.throwable?.message}")
            }

        return circuitBreaker
    }
    /**
     * Get retry instance and add event listeners for monitoring.
     * The retry itself is configured via application.yml
     */
    @Bean
    fun providerApiRetry(retryRegistry: RetryRegistry): Retry {
        val retry = retryRegistry.retry("providerApi")

        // Add event listeners for monitoring and logging
        retry.eventPublisher
            .onRetry { event ->
                logger.warn("Provider API retry attempt ${event.numberOfRetryAttempts}/${retry.retryConfig.maxAttempts} due to: ${event.lastThrowable?.message}")
            }
            .onSuccess { event ->
                if (event.numberOfRetryAttempts > 0) {
                    logger.info("Provider API succeeded after ${event.numberOfRetryAttempts} retries")
                }
            }
            .onError { event ->
                logger.error("Provider API failed after ${event.numberOfRetryAttempts} retries. Last error: ${event.lastThrowable?.message}")
            }

        return retry
    }

    @Bean
    fun providerApiTimeLimiter(timeLimiterRegistry: TimeLimiterRegistry): TimeLimiter {
        val timeLimiter = timeLimiterRegistry.timeLimiter("providerApi")

        timeLimiter.eventPublisher
            .onTimeout { event ->
                logger.warn("⏰ Provider API call timed out after ${timeLimiter.timeLimiterConfig.timeoutDuration}")
            }

        return timeLimiter
    }
}