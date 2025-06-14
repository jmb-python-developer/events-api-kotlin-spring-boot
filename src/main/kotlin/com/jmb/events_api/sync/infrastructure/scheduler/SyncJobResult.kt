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
    val totalPlans: Int,
    val successfulPlans: Int,
    val failedPlans: Int
)

data class BatchResult(
    val batchNumber: Int,
    val plansProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
)
