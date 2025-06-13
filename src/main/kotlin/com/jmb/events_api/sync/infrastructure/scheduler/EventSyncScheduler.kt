package com.jmb.events_api.sync.infrastructure.scheduler

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

@Component
@ConditionalOnProperty(
    prefix = "fever.sync",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class EventSyncScheduler(
    private val syncJobOrchestrator: SyncJobOrchestrator,
    private val dataSource: DataSource,
    @Value("\${fever.sync.enabled}") private val syncingEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(EventSyncScheduler::class.java)

    // Prevent overlapping executions
    private val syncInProgress = AtomicBoolean(false)

    //Default to 10 secs if not configured in application.yml
    @Scheduled(fixedDelayString = "\${fever.sync.interval}")
    fun scheduleEventSync() {
        if (!syncInProgress.compareAndSet(false, true)) {
            logger.warn("Plan sync already in progress, skipping this execution")
            return
        }

        try {
            logger.info("🔄 Scheduler triggering plan sync...")

            if (!syncingEnabled) {
                logger.info("⏸️ Plan sync process is disabled")
                return
            }

            // Simple database check
            if (!isDatabaseReady()) {
                logger.warn("⚠️ Database not ready - skipping plan sync")
                return
            }

            runBlocking {
                val result = syncJobOrchestrator.orchestrateFullSync()
                if (result.success) {
                    logger.info("Plan sync completed: ${result.successfulEvents}/${result.totalEvents} plans")
                } else {
                    logger.warn("Plan sync failed: ${result.errors.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            logger.error("Scheduled plan sync failed", e)
        } finally {
            syncInProgress.set(false)
        }
    }

    // Simple database readiness check
    private fun isDatabaseReady(): Boolean {
        return try {
            dataSource.connection.use { it.isValid(3) }
        } catch (e: Exception) {
            logger.warn("Database check failed: ${e.message}")
            false
        }
    }
}