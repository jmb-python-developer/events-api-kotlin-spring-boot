package com.jmb.events_api.sync.infrastructure.web

import com.jmb.events_api.sync.infrastructure.external.ProviderApiClient
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/demo/resilience")
class ResiliencePatternsDemoController(
    private val providerApiClient: ProviderApiClient
) {

    @GetMapping("/test-fetch")
    fun testProviderFetch(): Map<String, Any> = runBlocking {
        return@runBlocking try {
            val events = providerApiClient.fetchEvents()
            mapOf(
                "success" to true,
                "eventCount" to events.size,
                "circuitBreakerState" to providerApiClient.getCircuitBreakerState(),
                "retryMetrics" to providerApiClient.getRetryMetrics(),
                "events" to events.take(3).map {
                    mapOf(
                        "id" to it.baseEventId,
                        "title" to it.title,
                        "sellMode" to it.sellMode
                    )
                }
            )
        } catch (e: Exception) {
            mapOf(
                "success" to false,
                "error" to (e.message ?: "Unknown error"),
                "errorType" to e.javaClass.simpleName,
                "circuitBreakerState" to providerApiClient.getCircuitBreakerState(),
                "retryMetrics" to providerApiClient.getRetryMetrics()
            )
        }
    }

    @GetMapping("/health-check")
    fun testProviderHealth(): Map<String, Any> = runBlocking {
        val isHealthy = providerApiClient.healthCheck()
        mapOf(
            "providerHealthy" to isHealthy,
            "circuitBreakerState" to providerApiClient.getCircuitBreakerState(),
            "timestamp" to System.currentTimeMillis()
        )
    }

    @GetMapping("/circuit-breaker-status")
    fun getCircuitBreakerStatus(): Map<String, Any> {
        return mapOf(
            "state" to providerApiClient.getCircuitBreakerState(),
            "retryConfig" to providerApiClient.getRetryMetrics()
        )
    }
}