package com.jmb.events_api.sync.infrastructure.event

import com.jmb.events_api.sync.domain.event.PlanSyncedEvent
import com.jmb.events_api.sync.domain.event.PlanUpdatedEvent
import com.jmb.events_api.sync.domain.event.PlanFailedEvent
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
    fun handlePlanSynced(event: PlanSyncedEvent) {
        logger.info("Plan Synced: ${event.title} (ID: ${event.planEntityId.value})")
        // Here can integrate with Micrometer metrics - Skipped due to time constraints
        // meterRegistry.counter("plans.synced").increment()
    }

    @EventListener
    fun handlePlanUpdated(event: PlanUpdatedEvent) {
        logger.info("Plan updated: ${event.planEntityId.value}, price changed: ${event.newPriceRange}")
    }

    @EventListener
    fun handleSyncFailed(event: PlanFailedEvent) {
        logger.error("Sync failed for provider plan ID ${event.providerEventId}: ${event.failureReason}")
    }
}
