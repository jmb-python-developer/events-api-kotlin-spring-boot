package com.jmb.events_api.sync.infrastructure.scheduler

import kotlinx.coroutines.runBlocking
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

    //Default to 10 secs if not configured in application.yml
    @Scheduled(fixedDelayString = "\${fever.sync.interval:30000}")
    fun scheduleEventSync() {
        logger.info("Scheduler triggering ... ")
        //Could be an async process in the future, depending on the volumes
        runBlocking {
            syncJobOrchestrator.orchestrateFullSync()
        }
    }
}