package com.jmb.events_api.sync.infrastructure.scheduler

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicBoolean

@Component
@ConditionalOnProperty(
    prefix = "fever.sync",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class EventSyncScheduler(
    private val syncJobOrchestrator: SyncJobOrchestrator,
    @Value("\${fever.sync.enabled}") private val syncingEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(EventSyncScheduler::class.java)

    // Prevent overlapping executions
    private val syncInProgress = AtomicBoolean(false)

    //Default to 10 secs if not configured in application.yml
    @Scheduled(fixedDelayString = "\${fever.sync.interval}")
    fun scheduleEventSync() {
        // Prevent overlapping sync operations
        if (!syncInProgress.compareAndSet(false, true)) {
            logger.warn("Sync already in progress, skipping this execution")
            return
        }

        try {
            logger.info("Scheduler triggering sync...")
            if (syncingEnabled) {
                runBlocking {
                    syncJobOrchestrator.orchestrateFullSync()
                }
            } else {
                logger.info("Sync Process is disabled as per system properties")
            }
        } catch (e: Exception) {
            logger.error("Scheduled sync failed", e)
        } finally {
            syncInProgress.set(false)
        }
    }
}