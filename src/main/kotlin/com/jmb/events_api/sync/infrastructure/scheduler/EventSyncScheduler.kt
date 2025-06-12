package com.jmb.events_api.sync.infrastructure.scheduler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    prefix = "fever.sync",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class EventSyncScheduler(
    private val syncJobOrchestrator: SyncJobOrchestrator,
) {
    private val logger = LoggerFactory.getLogger(EventSyncScheduler::class.java)

    @Scheduled(fixedDelayString = "\${fever.sync.interval}")
    fun scheduleEventSync() {
        logger.info("Scheduler triggering ... ")
    }

    @Scheduled(cron = "0 */5 * * * *") // Every 5 minutes
    fun scheduleHealthCheck() {
        // TODO: Implement health check logic
        // Hint: Check provider availability, circuit breaker state
    }
}