package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import org.springframework.stereotype.Component

@Component
class PlanEntityMapper(
    private val zoneEntityMapper: ZoneEntityMapper
) {

    fun toDomain(entity: PlanJpaEntity): Plan {
        val planDateRange = DateRange(entity.planStartDate, entity.planEndDate)
        val priceRange = PriceRange(entity.priceRangeMin, entity.priceRangeMax)
        val sellPeriod = if (entity.sellFrom != null && entity.sellTo != null) {
            DateRange(entity.sellFrom!!, entity.sellTo!!)
        } else null
        val zones = entity.zones.map { zoneEntityMapper.toDomain(it) }

        return Plan.createOnline(
            id = PlanId.of(entity.id),
            title = entity.title,
            date = planDateRange,
            priceRange = priceRange,
            providerPlanId = entity.providerPlanId,
            organizerCompanyId = entity.organizerCompanyId,
            sellPeriod = sellPeriod,
            soldOut = entity.soldOut,
            lastUpdated = entity.lastUpdated,
            zones = zones,
            version = entity.version,
        )
    }

    fun toEntity(domain: Plan): PlanJpaEntity {
        return PlanJpaEntity(
            id = domain.id.value,
            providerPlanId = domain.providerPlanId,
            title = domain.title,
            planStartDate = domain.date.from,
            planEndDate = domain.date.to,
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellFrom = domain.sellPeriod?.from,
            sellTo = domain.sellPeriod?.to,
            soldOut = domain.soldOut,
            lastUpdated = domain.lastUpdated,
            version = domain.version,
        )
    }

    fun toEntityForUpdate(domain: Plan, existingEntity: PlanJpaEntity): PlanJpaEntity {
        return PlanJpaEntity(
            id = existingEntity.id,
            providerPlanId = existingEntity.providerPlanId,
            title = domain.title,
            planStartDate = domain.date.from,
            planEndDate = domain.date.to,
            priceRangeMin = domain.priceRange.min,
            priceRangeMax = domain.priceRange.max,
            sellMode = domain.sellMode.name,
            organizerCompanyId = domain.organizerCompanyId,
            sellFrom = domain.sellPeriod?.from,
            sellTo = domain.sellPeriod?.to,
            soldOut = domain.soldOut,
            lastUpdated = domain.lastUpdated,
            version = existingEntity.version,
        )
    }
}
