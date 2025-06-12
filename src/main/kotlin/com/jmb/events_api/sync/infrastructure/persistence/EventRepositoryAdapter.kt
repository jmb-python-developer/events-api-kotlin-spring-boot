package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.repository.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class EventRepositoryAdapter(
    private val eventJpaRepository: EventJpaRepository,
    private val eventEntityMapper: EventEntityMapper,
    private val zoneEntityMapper: ZoneEntityMapper,
): EventRepository {

    private val logger = LoggerFactory.getLogger(EventRepositoryAdapter::class.java)

    override fun upsertEvent(event: Event): Event {
        logger.debug("Saving event with providerId: ${event.providerEventId}")

        //Use upsert query instead of retrieving first the entity
        var entityToSave = eventEntityMapper.toEntity(event)
        val zones = event.zones.map { zoneEntityMapper.toEntity(it, entityToSave) }
        entityToSave.zones = zones
        val rowsAffected = eventJpaRepository.upsertEvent(entityToSave)

        logger.debug("Upsert affected $rowsAffected rows for provider ID: ${event.providerEventId}")
        return event
    }

    @Transactional(readOnly = true)
    override fun findById(eventId: String): Event? {
        logger.debug("Finding event by ID: $eventId")

        return eventJpaRepository.findById(eventId)
            .map { eventEntityMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByProviderId(providerId: String): Event? {
        logger.debug("Finding event by provider ID: $providerId")

        val entity = eventJpaRepository.findByProviderEventId(providerId)
        return entity?.let { eventEntityMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    fun findAll(): List<Event> {
        logger.debug("Finding all events")

        return eventJpaRepository.findAll()
            .map { eventEntityMapper.toDomain(it) }
    }

    @Transactional(readOnly = true)
    fun count(): Long {
        return eventJpaRepository.count()
    }


}
