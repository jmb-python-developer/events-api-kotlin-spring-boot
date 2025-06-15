package com.jmb.events_api.sync.infrastructure.scheduler

import java.time.Instant

data class SyncJobResult(
    val success: Boolean,
    val totalPlans: Int = 0,
    val successfulPlans: Int = 0,
    val failedPlans: Int = 0,
    val executionTimeMs: Long = 0,
    val startedAt: Instant = Instant.now(),
    val completedAt: Instant = Instant.now(),
    val errors: List<String> = emptyList()
)

data class BatchProcessingResult(
    val totalBatches: Int,
    val successfulBatches: Int,
    val failedBatches: Int,
    val partiallyFailedBatches: Int,
    val totalPlans: Int,
    val successfulPlans: Int,
    val failedPlans: Int,
    val mappingFailures: Int,
    val serviceFailures: Int
) {
    companion object {
        fun empty(): BatchProcessingResult {
            return BatchProcessingResult(
                totalBatches = 0,
                successfulBatches = 0,
                failedBatches = 0,
                partiallyFailedBatches = 0,
                totalPlans = 0,
                successfulPlans = 0,
                failedPlans = 0,
                mappingFailures = 0,
                serviceFailures = 0
            )
        }

        fun fromBatchResults(batchResults: List<BatchResult>, totalPlans: Int): BatchProcessingResult {
            return BatchProcessingResult(
                totalBatches = batchResults.size,
                successfulBatches = batchResults.count { it.isSuccessful },
                failedBatches = batchResults.count { it.errorCount == it.plansProcessed }, // Complete failures
                partiallyFailedBatches = batchResults.count { it.hasPartialFailures },
                totalPlans = totalPlans,
                successfulPlans = batchResults.sumOf { it.successCount },
                failedPlans = batchResults.sumOf { it.errorCount },
                mappingFailures = batchResults.sumOf { it.mappingFailures },
                serviceFailures = batchResults.sumOf { it.serviceFailures }
            )
        }
    }
}

data class BatchResult(
    val batchNumber: Int,
    val plansProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val mappingFailures: Int = 0,
    val serviceFailures: Int = 0,
) {
    val isSuccessful: Boolean = errorCount == 0
    val hasPartialFailures: Boolean = errorCount > 0 && successCount > 0
}
