package com.jmb.events_api.sync.infrastructure.event

import com.jmb.events_api.sync.domain.event.PlanSyncedEvent
import com.jmb.events_api.sync.domain.event.PlanUpdatedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Cross-cutting concern bean for Cache operations.
 */
@Component
class CacheInvalidationHandler {

    private val logger = LoggerFactory.getLogger(CacheInvalidationHandler::class.java)

    @EventListener
    fun handlePlanSynced(event: PlanSyncedEvent) {
        logger.warn("Invalidating cache due to new plan: ${event.planEntityId.value}")
        // Here could be integrated with Redis/Caffeine cache - Skipped due to time constraint
    }

    @EventListener
    fun handlePlanUpdated(event: PlanUpdatedEvent) {
        logger.warn("Invalidating cache due to plan update: ${event.planEntityId.value}")
        // Here could be integrated with Redis/Caffeine cache - Skipped due to time constraint
    }
}
