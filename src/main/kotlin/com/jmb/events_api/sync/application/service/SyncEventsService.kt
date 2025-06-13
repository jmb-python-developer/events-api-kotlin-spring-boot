package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.EventSyncedEvent
import com.jmb.events_api.sync.domain.event.EventUpdatedEvent
import com.jmb.events_api.sync.domain.event.SyncFailedEvent
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.repository.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Contains business logic to sync plans by calling the necessary ports. Called by Infrastructure objects.
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
     * Process single plan with optimistic locking and retry mechanism
     * Called by SyncBatchProcessor for each plan
     */
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    private suspend fun processEvent(event: Event): Event? {
        return try {
            // Check if plan already exists
            val existingEvent = eventRepository.findByProviderId(event.providerEventId)

            if (existingEvent != null) {
                // Update existing plan
                updateExistingEvent(existingEvent, event)
            } else {
                // Create new plan
                createNewEvent(event)
            }

        } catch (e: OptimisticLockingFailureException) {
            logger.warn("Optimistic locking failure for plan ${event.providerEventId}, retrying...")
            throw e // Will trigger retry functionality
        } catch (e: Exception) {
            logger.error("Failed to process plan ${event.providerEventId}", e)
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
        logger.debug("Created new plan: {} ({})", savedEvent.title, savedEvent.providerEventId)
        return savedEvent
    }

    /**
     * Update existing plan with change detection and domain events
     */
    private suspend fun updateExistingEvent(existingEvent: Event, event: Event): Event {
        // Detect changes for domain events
        val hasChanges = detectAndLogChanges(existingEvent, event)

        if (!hasChanges) {
            logger.debug("No significant changes for plan: ${event.title}")
            return existingEvent
        }

        // Create updated event
        val updatedEvent = existingEvent.updateFromProvider(
            newTitle = event.title,
            date = event.date,
            newPriceRange = event.priceRange,
            newSellFrom = event.sellPeriod?.from ?: event.date.from,
            newSellTo = event.sellPeriod?.to ?: event.date.to,
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
        logger.debug("Updated plan: ${event.title} (${event.providerEventId})")
        return savedEvent
    }

    /**
     * Detect significant changes that warrant domain events
     */
    private fun detectSignificantChanges(
        existingEvent: Event,
        newEvent: Event
    ): Boolean {
        return existingEvent.title != newEvent.title ||
                existingEvent.priceRange.min.compareTo(newEvent.priceRange.min) != 0 ||
                existingEvent.priceRange.max.compareTo(newEvent.priceRange.max) != 0 ||
                existingEvent.soldOut != newEvent.soldOut ||
                existingEvent.organizerCompanyId != newEvent.organizerCompanyId ||
                existingEvent.date != newEvent.date ||
                existingEvent.sellPeriod != newEvent.sellPeriod
    }

    private fun detectAndLogChanges(
        existingEvent: Event,
        newEvent: Event
    ): Boolean {
        val changes = mutableListOf<String>()

        // Title changes
        if (existingEvent.title != newEvent.title) {
            changes.add("title: '${existingEvent.title}' → '${newEvent.title}'")
        }

        // Price changes (with precision handling)
        if (!existingEvent.priceRange.isEquivalentTo(newEvent.priceRange)) {
            changes.add("price: ${existingEvent.priceRange.min}-${existingEvent.priceRange.max} → ${newEvent.priceRange.min}-${newEvent.priceRange.max}")
        }

        // Date changes (with minute precision)
        if (!existingEvent.date.isEquivalentTo(newEvent.date)) {
            changes.add("dates: ${existingEvent.date.from} to ${existingEvent.date.to} → ${newEvent.date.from} to ${newEvent.date.to}")
        }

        // Sold out status
        if (existingEvent.soldOut != newEvent.soldOut) {
            changes.add("soldOut: ${existingEvent.soldOut} → ${newEvent.soldOut}")
        }

        // Organizer company
        if (existingEvent.organizerCompanyId != newEvent.organizerCompanyId) {
            changes.add("organizer: '${existingEvent.organizerCompanyId}' → '${newEvent.organizerCompanyId}'")
        }

        // Sell period changes
        if (!sellPeriodsEquivalent(existingEvent.sellPeriod, newEvent.sellPeriod)) {
            changes.add("sellPeriod: ${existingEvent.sellPeriod} → ${newEvent.sellPeriod}")
        }

        // Log changes if any detected
        if (changes.isNotEmpty()) {
            logger.info("📝 Plan changes detected for '${newEvent.title}' (${newEvent.providerEventId}): ${changes.joinToString(", ")}")
            return true
        }

        logger.debug("✅ No significant changes for plan '${newEvent.title}' (${newEvent.providerEventId})")
        return false
    }

    private fun sellPeriodsEquivalent(period1: DateRange?, period2: DateRange?): Boolean {
        return when {
            period1 == null && period2 == null -> true
            period1 == null || period2 == null -> false
            else -> period1.isEquivalentTo(period2)
        }
    }
}