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
        logger.info("Invalidating cache due to new plan: ${event.planEntityId.value}")
        // Future: integrate with Redis/Caffeine cache
    }

    @EventListener
    fun handlePlanUpdated(event: PlanUpdatedEvent) {
        logger.info("Invalidating cache due to plan update: ${event.planEntityId.value}")
        // Future: selective cache invalidation
    }
}