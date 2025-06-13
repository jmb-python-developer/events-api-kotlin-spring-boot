package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.EventSyncedEvent
import com.jmb.events_api.sync.domain.event.EventUpdatedEvent
import com.jmb.events_api.sync.domain.event.SyncFailedEvent
import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.model.PriceRange
import com.jmb.events_api.sync.domain.repository.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Contains business logic to sync events by calling the necessary ports. Called by Infrastructure objects.
 *
 * Triggering flow:
 *
 * ScheduledJob (Infrastructure)
 *     → calls SyncEventsService (Application)
 *         → uses EventRepositoryPort + ProviderClientPort (Domain interfaces)
 *             → implemented by JPA + HTTP adapters (Infrastructure)
 */
@Service
@Transactional
class SyncEventsService(
    private val eventRepository: EventRepository,
    private val domainEventPublisher: DomainEventPublisher
) {

    private val logger = LoggerFactory.getLogger(SyncEventsService::class.java)

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    suspend fun syncEvents(events: Collection<Event>): Collection<Event> {
        return events.mapNotNull { event -> processEvent(event) }
    }

    /**
     * Process single event with optimistic locking and retry mechanism
     * Called by SyncBatchProcessor for each event
     */
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    private suspend fun processEvent(event: Event): Event? {
        return try {
            // Check if event already exists
            val existingEvent = eventRepository.findByProviderId(event.providerEventId)

            if (existingEvent != null) {
                // Update existing event
                updateExistingEvent(existingEvent, event)
            } else {
                // Create new event
                createNewEvent(event)
            }

        } catch (e: OptimisticLockingFailureException) {
            logger.warn("Optimistic locking failure for event ${event.providerEventId}, retrying...")
            throw e // Will trigger retry functionality
        } catch (e: Exception) {
            logger.error("Failed to process event ${event.providerEventId}", e)
            // Publish failure event
            domainEventPublisher.publish(
                SyncFailedEvent(
                    providerEventId = event.providerEventId,
                    failureReason = e.message ?: "Unknown error"
                )
            )
            null
        }
    }

    private suspend fun createNewEvent(event: Event): Event {
        val savedEvent = eventRepository.upsertEvent(event)

        // Publish domain event
        domainEventPublisher.publish(
            EventSyncedEvent(
                eventEntityId = savedEvent.id,
                title = savedEvent.title,
                providerEventId = savedEvent.providerEventId,
                organizerCompanyId = savedEvent.organizerCompanyId,
                eventDate = savedEvent.date,
                priceRange = savedEvent.priceRange,
                sellPeriod = savedEvent.sellPeriod,
                soldOut = savedEvent.soldOut,
                version = savedEvent.version
            )
        )
        logger.debug("Created new event: {} ({})", savedEvent.title, savedEvent.providerEventId)
        return savedEvent
    }

    /**
     * Update existing event with change detection and domain events
     */
    private suspend fun updateExistingEvent(existingEvent: Event, event: Event): Event {
        // Detect changes for domain events
        val hasChanges = detectSignificantChanges(existingEvent, event, event.priceRange)

        if (!hasChanges) {
            logger.debug("No significant changes for event: ${event.title}")
            return existingEvent
        }

        // Create updated event
        val updatedEvent = existingEvent.updateFromProvider(
            newTitle = event.title,
            date = event.date,
            newPriceRange = event.priceRange,
            newSellFrom = event.date.sellFrom,
            newSellTo = event.date.sellTo,
            newSoldOut = event.soldOut,
            newOrganizerCompanyId = event.organizerCompanyId,
            newZones = event.zones,
        )

        val savedEvent = eventRepository.upsertEvent(updatedEvent)

        // Publish update event with detailed change information
        domainEventPublisher.publish(
            EventUpdatedEvent(
                eventEntityId = savedEvent.id,
                providerEventId = savedEvent.providerEventId,
                previousVersion = existingEvent.version,
                newVersion = savedEvent.version,
                previousTitle = existingEvent.title,
                newTitle = savedEvent.title,
                previousPriceRange = existingEvent.priceRange,
                newPriceRange = savedEvent.priceRange,
                previousSellPeriod = existingEvent.sellPeriod,
                newSellPeriod = savedEvent.sellPeriod,
                previousSoldOut = existingEvent.soldOut,
                newSoldOut = savedEvent.soldOut,
                previousOrganizerCompanyId = existingEvent.organizerCompanyId,
                newOrganizerCompanyId = savedEvent.organizerCompanyId
            )
        )
        logger.debug("Updated event: ${event.title} (${event.providerEventId})")
        return savedEvent
    }

    /**
     * Detect significant changes that warrant domain events
     */
    private fun detectSignificantChanges(
        existingEvent: Event,
        event: Event,
        newPriceRange: PriceRange
    ): Boolean {
        return existingEvent.title != event.title ||
                existingEvent.priceRange.min != newPriceRange.min ||
                existingEvent.priceRange.max != newPriceRange.max ||
                existingEvent.soldOut != event.soldOut ||
                existingEvent.organizerCompanyId != event.organizerCompanyId ||
                existingEvent.date.sellFrom != event.date.sellFrom ||
                existingEvent.date.sellTo != event.date.sellTo
    }
}
