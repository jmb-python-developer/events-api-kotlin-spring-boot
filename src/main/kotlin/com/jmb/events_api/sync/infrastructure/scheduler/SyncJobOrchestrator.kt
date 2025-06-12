package com.jmb.events_api.sync.infrastructure.scheduler

import com.jmb.events_api.sync.infrastructure.external.ProviderApiClient
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncJobOrchestrator(
    private val providerApiClient: ProviderApiClient,
    private val syncBatchProcessor: SyncBatchProcessor,
) {
    private val logger = LoggerFactory.getLogger(SyncJobOrchestrator::class.java)

    suspend fun orchestrateFullSync(): SyncJobResult {
        // 1. Check circuit breaker state first
        if (circuitBreakerCheck() == CircuitBreaker.State.CLOSED) {
            // 2. Fetch events from provider - Actual External API call
            val events = providerApiClient.fetchEvents()
            // 3. Process in batches using SyncBatchProcessor
            syncBatchProcessor.processBatch(events = events, batchSize = 20)
            // 4. Return comprehensive result with metrics
        }
        return SyncJobResult(success = false, errors = listOf("Circuit Breaker is OPEN"))
    }

    fun circuitBreakerCheck(): CircuitBreaker.State {
        val state = providerApiClient.getCircuitBreakerState()
        when(state) {
            CircuitBreaker.State.CLOSED -> {
                logger.info("Circuit breaker healthy - proceeding with sync")
            }
            CircuitBreaker.State.OPEN -> {
                logger.warn("Circuit breaker OPEN - skipping sync")
            }
            CircuitBreaker.State.HALF_OPEN -> {
                logger.info("Circuit breaker testing recovery")
            }
            else -> logger.info("Other States of Circuit Breaker are untracked")
        }
        return state
    }

}