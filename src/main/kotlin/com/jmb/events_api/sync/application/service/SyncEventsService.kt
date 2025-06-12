package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.port.out.ProviderClientPort
import com.jmb.events_api.sync.domain.repository.EventRepository
import org.slf4j.LoggerFactory
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
        private const val MAX_BATCH_SIZE = 50
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    fun syncEvents(events: Collection<Event>): Collection<Event> {
        //TODO: Implement this after ports are done.
        return emptyList()
    }
}
