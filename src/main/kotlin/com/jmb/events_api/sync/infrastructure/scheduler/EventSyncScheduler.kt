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
class PlanSyncScheduler(
    private val syncJobOrchestrator: SyncJobOrchestrator,
    private val dataSource: DataSource,
    @Value("\${fever.sync.enabled}") private val syncingEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(PlanSyncScheduler::class.java)

    // Prevent overlapping executions
    private val syncInProgress = AtomicBoolean(false)

    companion object {
        private const val DB_CHECK_TIMEOUT_SECONDS = 3
    }

    // Default to interval defined in application.yml
    @Scheduled(fixedDelayString = "\${fever.sync.interval}")
    fun schedulePlanSync() {
        if (!syncingEnabled) {
            logger.info("⏸️ Plan sync process is disabled")
            return
        }

        if (!syncInProgress.compareAndSet(false, true)) {
            logger.warn("⏳ Plan sync already in progress, skipping this execution")
            return
        }

        try {
            logger.info("🔄 Scheduler triggering plan sync...")

            if (!isDatabaseReady()) {
                logger.warn("⚠️ Database not ready - skipping plan sync")
                return
            }

            runBlocking {
                val result = syncJobOrchestrator.orchestrateFullSync()
                if (result.success) {
                    logger.info("Plan sync completed: ${result.successfulPlans}/${result.totalPlans} successful")
                } else {
                    logger.warn("Plan sync failed with errors: ${result.errors.joinToString(", ")}")
                }
            }
        } catch (e: Exception) {
            logger.error("Scheduled plan sync failed", e)
        } finally {
            syncInProgress.set(false)
        }
    }

    private fun isDatabaseReady(): Boolean {
        return try {
            dataSource.connection.use { it.isValid(DB_CHECK_TIMEOUT_SECONDS) }
        } catch (e: Exception) {
            logger.warn("Database check failed: ${e.message}")
            false
        }
    }
}
