package com.jmb.events_api.sync.infrastructure.scheduler

import java.time.Instant

data class SyncJobResult(
    val success: Boolean,
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int,
    val executionTimeMs: Long,
    val startedAt: Instant,
    val completedAt: Instant,
    val errors: List<String> = emptyList()
)

data class HealthSyncResult(
    val providerHealthy: Boolean,
    val circuitBreakerState: String,
    val responseTimeMs: Long,
    val checkedAt: Instant
)

data class BatchProcessingResult(
    val totalBatches: Int,
    val successfulBatches: Int,
    val failedBatches: Int,
    val totalEvents: Int,
    val successfulEvents: Int,
    val failedEvents: Int
)

data class BatchResult(
    val batchNumber: Int,
    val eventsProcessed: Int,
    val successCount: Int,
    val errorCount: Int,
    val errors: List<String> = emptyList()
)