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
        // Check if entity exists
        val existingEntity = eventJpaRepository.findByProviderEventId(event.providerEventId)

        val savedEntity = if (existingEntity != null) {
            // UPDATE: Use the existing entity's ID and version
            val entityToUpdate = eventEntityMapper.toEntityForUpdate(event, existingEntity)
            setupZones(entityToUpdate, event)
            eventJpaRepository.save(entityToUpdate)
        } else {
            // INSERT: Create new entity
            val entityToSave = eventEntityMapper.toEntity(event)
            setupZones(entityToSave, event)
            eventJpaRepository.save(entityToSave)
        }

        logger.debug("Upserted event: ${event.title} with ${savedEntity.zones.size} zones")
        return eventEntityMapper.toDomain(savedEntity)
    }

    private fun setupZones(entity: EventJpaEntity, event: Event) {
        entity.zones.clear()
        val zones = event.zones.map { zone ->
            val zoneEntity = zoneEntityMapper.toEntity(zone, entity)
            zoneEntity.event = entity
            zoneEntity
        }
        entity.zones.addAll(zones)
    }

    @Transactional(readOnly = true)
    override fun findById(eventId: String): Event? {
        return eventJpaRepository.findById(eventId)
            .map { eventEntityMapper.toDomain(it) }
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun findByProviderId(providerId: String): Event? {
        val entity = eventJpaRepository.findByProviderEventId(providerId)
        return entity?.let { eventEntityMapper.toDomain(it) }
    }
}