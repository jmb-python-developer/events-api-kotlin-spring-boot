package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EventEntityMapper(
    private val zoneEntityMapper: ZoneEntityMapper
) {

    fun toDomain(entity: EventJpaEntity): Event {
        // Event timing (when the show actually happens)
        val eventDateRange = DateRange(entity.planStartDate, entity.planEndDate)
        val priceRange = PriceRange(entity.priceRangeMin, entity.priceRangeMax)

        // Sales timing (when tickets are sold) - this is the sell period
        val sellPeriod = if (entity.sellFrom != null && entity.sellTo != null) {
            DateRange(entity.sellFrom!!, entity.sellTo!!)
        } else null

        val zones = entity.zones.map { zoneEntityMapper.toDomain(it) }
        return Event.createOnline(
            id = EventId.of(entity.id),
            title = entity.title,
            date = eventDateRange,  // When the event happens
            priceRange = priceRange,
            providerEventId = entity.providerEventId,
            organizerCompanyId = entity.organizerCompanyId,
            sellPeriod = sellPeriod,  // When tickets are sold
            soldOut = entity.soldOut,
            lastUpdated = entity.lastUpdated,
            zones = zones,
            version = entity.version,
        )
    }

    fun toEntity(domain: Event): EventJpaEntity {
        return EventJpaEntity(
            id = domain.id.value,
            providerEventId = domain.providerEventId,
            title = domain.title,
            planStartDate = domain.date.from,      // Event timing
            planEndDate = domain.date.to,          // Event timing
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellFrom = domain.sellPeriod?.from,    // Sales timing
            sellTo = domain.sellPeriod?.to,        // Sales timing
            soldOut = domain.soldOut,
            lastUpdated = domain.lastUpdated,
            version = domain.version,
        )
    }

    fun toEntityForUpdate(domain: Event, existingEntity: EventJpaEntity): EventJpaEntity {
        return EventJpaEntity(
            id = existingEntity.id,
            providerEventId = existingEntity.providerEventId,
            title = domain.title,
            planStartDate = domain.date.from,      // Event timing
            planEndDate = domain.date.to,          // Event timing
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellFrom = domain.sellPeriod?.from,    // Sales timing
            sellTo = domain.sellPeriod?.to,        // Sales timing
            soldOut = domain.soldOut,
            lastUpdated = domain.lastUpdated,
            version = existingEntity.version, //Needed for JPA Optimistic Locking and Version automanagement
        )
    }
}
