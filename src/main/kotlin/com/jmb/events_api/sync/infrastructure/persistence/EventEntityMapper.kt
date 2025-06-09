package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class EventEntityMapper(
    private val zoneEntityMapper: ZoneEntityMapper
) {

    fun toDomain(entity: EventJpaEntity): Event {
        val dateRange = DateRange(entity.sellFrom, entity.sellTo)
        val priceRange = PriceRange(entity.priceRangeMin, entity.priceRangeMax)

        val sellPeriod = if (entity.sellPeriodFrom != null && entity.sellPeriodTo != null) {
            DateRange(entity.sellPeriodFrom!!, entity.sellPeriodTo!!)
        } else null

        return Event.createOnline(
            id = EventId.of(entity.id),
            title = entity.title,
            date = dateRange,
            priceRange = priceRange,
            providerEventId = entity.providerEventId,
            organizerCompanyId = entity.organizerCompanyId,
            sellPeriod = sellPeriod,
            soldOut = entity.soldOut,
            lastUpdated = entity.lastUpdated,
            version = entity.version
        )
    }

    fun toEntity(domain: Event): EventJpaEntity {
        return EventJpaEntity(
            id = domain.id.value,
            providerEventId = domain.providerEventId,
            title = domain.title,
            sellFrom = domain.date.sellFrom,
            sellTo = domain.date.sellTo,
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellPeriodFrom = domain.sellPeriod?.sellFrom,
            sellPeriodTo = domain.sellPeriod?.sellTo,
            soldOut = domain.soldOut,
            lastUpdated = domain.lastUpdated,
            version = domain.version
        )
    }

    fun updateEntity(entity: EventJpaEntity, domain: Event): EventJpaEntity {
        return EventJpaEntity(
            id = entity.id,
            providerEventId = entity.providerEventId,
            title = domain.title,
            sellFrom = domain.date.sellFrom,
            sellTo = domain.date.sellTo,
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellPeriodFrom = domain.sellPeriod?.sellFrom,
            sellPeriodTo = domain.sellPeriod?.sellTo,
            soldOut = domain.soldOut,
            lastUpdated = Instant.now(),
            version = domain.version,
            zones = entity.zones
        )
    }
}