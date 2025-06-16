package com.jmb.events_api.sync.infrastructure.config

import com.fasterxml.jackson.core.JsonProcessingException
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
                logger.info("Provider Plan API Circuit Breaker: ${event.stateTransition}")
            }
            .onFailureRateExceeded { event ->
                logger.warn("Provider Plan API failure rate exceeded: ${event.failureRate}%")
            }
            .onCallNotPermitted { event ->
                logger.warn("Provider Plan API call blocked - Circuit Breaker is OPEN")
            }
            .onSuccess { event ->
                logger.debug("Provider Plan API call succeeded")
            }
            .onError { event ->
                logger.debug("Provider Plan API call failed: ${event.throwable?.message}")
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

        retry.eventPublisher
            .onRetry { event ->
                val exception = event.lastThrowable
                val context = when {
                    exception?.message?.contains("503") == true -> "Provider server error (503) - likely temporary overload"
                    exception?.message?.contains("timeout") == true -> "Call timeout - provider is responding slowly"
                    exception?.message?.contains("Connection") == true -> "Network connectivity issue"
                    exception is JsonProcessingException -> "XML parsing failed - data format issue"
                    else -> "Unknown error: ${exception?.javaClass?.simpleName}"
                }

                logger.warn("Retrying provider API call (attempt ${event.numberOfRetryAttempts}/${retry.retryConfig.maxAttempts}) due to: $context")
            }
            .onSuccess { event ->
                if (event.numberOfRetryAttempts > 0) {
                    logger.info("Provider API succeeded after ${event.numberOfRetryAttempts} retries - provider recovered")
                }
            }
            .onError { event ->
                val finalError = event.lastThrowable
                logger.error("Provider API failed permanently after ${event.numberOfRetryAttempts} retries. Final error: ${finalError?.javaClass?.simpleName}: ${finalError?.message}")
            }

        return retry
    }

    @Bean
    fun providerApiTimeLimiter(timeLimiterRegistry: TimeLimiterRegistry): TimeLimiter {
        val timeLimiter = timeLimiterRegistry.timeLimiter("providerApi")

        timeLimiter.eventPublisher
            .onTimeout { event ->
                logger.warn("⏰ Provider Plan API call timed out after ${timeLimiter.timeLimiterConfig.timeoutDuration}")
            }

        return timeLimiter
    }
}
