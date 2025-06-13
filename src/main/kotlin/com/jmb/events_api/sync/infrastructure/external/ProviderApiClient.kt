package com.jmb.events_api.sync.infrastructure.external

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.jmb.events_api.sync.application.dto.ProviderPlanDto
import com.jmb.events_api.sync.domain.port.out.ProviderClientPort
import com.jmb.events_api.sync.domain.port.out.ProviderProperties
import com.jmb.events_api.sync.infrastructure.config.ProviderConfig
import com.jmb.events_api.sync.infrastructure.external.dto.PlanListResponseDto
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
    private val providerConfig: ProviderConfig,
    private val circuitBreaker: CircuitBreaker,
    private val retry: Retry,
    private val timeLimiter: TimeLimiter,
) : ProviderClientPort {

    private val logger = LoggerFactory.getLogger(ProviderApiClient::class.java)
    private val xmlMapper: XmlMapper = providerConfig.xmlMapper()

    override suspend fun fetchPlans(): List<ProviderPlanDto> {
        val startTime = Instant.now()

        return try {
            logger.info("Fetching plans from provider: ${providerProperties.url}")

            val plans = timeLimiter.executeSuspendFunction {
                circuitBreaker.executeSuspendFunction {
                    retry.executeSuspendFunction {
                        fetchPlansFromProvider()
                    }
                }
            }

            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.info("Successfully fetched ${plans.size} plans in ${duration.toMillis()}ms")

            plans

        } catch (e: Exception) {
            val duration = java.time.Duration.between(startTime, Instant.now())
            logger.error("Failed to fetch plans after ${duration.toMillis()}ms", e)

            throw when (e) {
                is ProviderApiException -> e
                is io.github.resilience4j.circuitbreaker.CallNotPermittedException -> {
                    ProviderApiException.circuitBreakerOpen("Circuit breaker is open - provider may be down")
                }
                is java.util.concurrent.TimeoutException -> {
                    ProviderApiException.timeout(e)
                }
                else -> ProviderApiException.networkError(e)
            }
        }
    }

    private suspend fun fetchPlansFromProvider(): List<ProviderPlanDto> = withContext(Dispatchers.IO) {
        try {
            logger.debug("Making HTTP call to provider API")
            val xmlContent = restTemplate.getForObject(providerProperties.url, String::class.java)
                ?: throw ProviderApiException.invalidResponse("Empty response from provider")
            logger.debug("Received XML response, parsing...")
            val planListResponse = xmlMapper.readValue(xmlContent, PlanListResponseDto::class.java)
            val plans = planListResponse.toCleanPlans()
            logger.debug("Parsed ${plans.size} online plans from provider response")
            plans
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
     * Get circuit breaker state for monitoring
     */
    fun getCircuitBreakerState(): CircuitBreaker.State = circuitBreaker.state
}