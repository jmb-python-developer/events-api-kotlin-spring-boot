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
        try {
            val state = circuitBreakerCheck()

            return when (state) {
                CircuitBreaker.State.OPEN,
                CircuitBreaker.State.FORCED_OPEN -> {
                    logger.warn("Circuit breaker is $state - skipping plan sync")
                    SyncJobResult(success = false, errors = listOf("Circuit Breaker is $state"))
                }
                CircuitBreaker.State.CLOSED,
                CircuitBreaker.State.HALF_OPEN,
                CircuitBreaker.State.DISABLED,
                CircuitBreaker.State.METRICS_ONLY -> {
                    logger.info("Circuit breaker is $state - proceeding with plan sync")
                    try {
                        val plans = providerApiClient.fetchPlans()
                        val batchResult = syncBatchProcessor.processBatch(plans, batchSize = 20)

                        SyncJobResult(
                            success = true,
                            totalPlans = batchResult.totalPlans,
                            successfulPlans = batchResult.successfulPlans,
                            failedPlans = batchResult.failedPlans
                        )
                    } catch (e: Exception) {
                        logger.error("Plan sync failed in state $state", e)
                        SyncJobResult(success = false, errors = listOf(e.message ?: "Unknown error"))
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Plan sync failed due to unresponsive circuit breaker", e)
            return SyncJobResult(success = false, errors = listOf(e.message ?: "Circuit breaker check failed"))
        }
    }

    fun circuitBreakerCheck(): CircuitBreaker.State {
        val state = providerApiClient.getCircuitBreakerState()
        when (state) {
            CircuitBreaker.State.CLOSED -> {
                logger.info("Circuit breaker healthy - proceeding with plan sync")
            }

            CircuitBreaker.State.OPEN -> {
                logger.warn("Circuit breaker OPEN - skipping plan sync")
            }

            CircuitBreaker.State.HALF_OPEN -> {
                logger.info("Circuit breaker testing recovery for plan sync")
            }

            else -> logger.info("Other States of Circuit Breaker are untracked")
        }
        return state
    }
}
