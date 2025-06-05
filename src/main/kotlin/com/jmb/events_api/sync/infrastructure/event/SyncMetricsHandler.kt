package com.jmb.events_api.sync.infrastructure.event

import com.jmb.events_api.sync.domain.event.EventSyncedEvent
import com.jmb.events_api.sync.domain.event.EventUpdatedEvent
import com.jmb.events_api.sync.domain.event.SyncFailedEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Cross-cutting concern bean for Logging/Observability/Metrics
 */
@Component
class SyncMetricsHandler {

    private val logger = LoggerFactory.getLogger(SyncMetricsHandler::class.java)

    @EventListener
    fun handleEventSynced(event: EventSyncedEvent) {
        logger.info("Event Synced: ${event.title} (ID: ${event.eventEntityId.value})")
        // Here you could integrate with Micrometer metrics
        // meterRegistry.counter("events.synced").increment()
    }

    @EventListener
    fun handleEventUpdated(event: EventUpdatedEvent) {
        logger.info("Event updated: ${event.eventEntityId.value}, price changed: ${event.newPriceRange}")
    }

    @EventListener
    fun handleSyncFailed(event: SyncFailedEvent) {
        logger.error("Sync failed for provider ID ${event.providerEventId}: ${event.failureReason}")
    }
}