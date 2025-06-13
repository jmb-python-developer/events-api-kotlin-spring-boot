package com.jmb.events_api.sync.infrastructure.event

import com.jmb.events_api.sync.domain.event.EventSyncedEvent
import com.jmb.events_api.sync.domain.event.EventUpdatedEvent
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
    fun handleEventSynced(event: EventSyncedEvent) {
        logger.info("Invalidating cache due to new plan: ${event.eventEntityId.value}")
        // Future: integrate with Redis/Caffeine cache
    }

    @EventListener
    fun handleEventUpdated(event: EventUpdatedEvent) {
        logger.info("Invalidating cache due to plan update: ${event.eventEntityId.value}")
        // Future: selective cache invalidation
    }
}