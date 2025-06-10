package com.jmb.events_api.sync.infrastructure.external

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.jmb.events_api.sync.application.dto.ProviderEventDto
import com.jmb.events_api.sync.domain.port.out.ProviderClientPort
import com.jmb.events_api.sync.domain.port.out.ProviderProperties
import com.jmb.events_api.sync.infrastructure.external.dto.EventListResponseDto
import com.jmb.events_api.sync.infrastructure.external.exception.ProviderApiException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.kotlin.timelimiter.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.Instant

@Component
class ProviderApiClient(
    private val restTemplate: RestTemplate,
    private val providerProperties: ProviderProperties,
    private val xmlMapper: XmlMapper,
    private val circuitBreaker: CircuitBreaker,
    private val retry: Retry,
    private val timeLimiter: TimeLimiter,
) : ProviderClientPort {

    private val logger = LoggerFactory.getLogger(ProviderApiClient::class.java)

    override suspend fun fetchEvents(): List<ProviderEventDto> {
        val startTime = Instant.now()

        return try {
            logger.info("Fetching events from provider: ${providerProperties.url}")

            // WRAP with TimeLimiter:
            val events = timeLimiter.executeSuspendFunction {
                circuitBreaker.executeSuspendFunction {
                    retry.executeSuspendFunction {
                        fetchEventsFromProvider()
                    }
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.info("Successfully fetched ${events.size} events in ${duration.toMillis()}ms")

            events

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.error("Failed to fetch events after ${duration.toMillis()}ms", e)

            throw when (e) {
                is ProviderApiException -> e
                is io.github.resilience4j.circuitbreaker.CallNotPermittedException -> {
                    ProviderApiException.circuitBreakerOpen("Circuit breaker is open - provider may be down")
                }
                is java.util.concurrent.TimeoutException -> {  // ADD THIS
                    ProviderApiException.timeout(e)
                }
                else -> ProviderApiException.networkError(e)
            }
        }
    }

    private suspend fun fetchEventsFromProvider(): List<ProviderEventDto> = withContext(Dispatchers.IO) {
        try {
            logger.debug("Making HTTP call to provider API")

            val xmlContent = restTemplate.getForObject(providerProperties.url, String::class.java)
                ?: throw ProviderApiException.invalidResponse("Empty response from provider")

            logger.debug("Received XML response, parsing...")

            val eventListResponse = xmlMapper.readValue(xmlContent, EventListResponseDto::class.java)
            val events = eventListResponse.toCleanEvents()

            logger.debug("Parsed ${events.size} online events from provider response")

            events

        } catch (e: Exception) {
            when (e) {
                is ProviderApiException -> throw e
                is com.fasterxml.jackson.core.JsonProcessingException -> {
                    throw ProviderApiException.parseError(e)
                }

                is org.springframework.web.client.HttpClientErrorException -> {
                    throw ProviderApiException.httpError(e.statusCode.value(), e.responseBodyAsString)
                }

                is org.springframework.web.client.HttpServerErrorException -> {
                    throw ProviderApiException.httpError(e.statusCode.value(), e.responseBodyAsString)
                }

                is org.springframework.web.client.ResourceAccessException -> {
                    throw ProviderApiException.networkError(e)
                }

                else -> {
                    throw ProviderApiException.unexpectedError(e)
                }
            }
        }
    }

    /**
     * Health check method for monitoring
     */
    suspend fun healthCheck(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simple connectivity test
            val response = restTemplate.optionsForAllow(providerProperties.url)
            true
        } catch (e: Exception) {
            logger.warn("Provider health check failed", e)
            false
        }
    }

    /**
     * Get circuit breaker state for monitoring
     */
    fun getCircuitBreakerState(): String = circuitBreaker.state.name

    /**
     * Get retry metrics for monitoring
     */
    fun getRetryMetrics(): Map<String, Any> = mapOf(
        "maxAttempts" to retry.retryConfig.maxAttempts,
        "waitDuration" to (retry.retryConfig.intervalFunction?.apply(1) ?: 0L),
        "name" to retry.name
    )
}
