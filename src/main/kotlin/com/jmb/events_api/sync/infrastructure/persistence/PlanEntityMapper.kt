package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PlanEntityMapper(
    private val zoneEntityMapper: ZoneEntityMapper
) {

    fun toDomain(entity: PlanJpaEntity): Plan {
        // Plan timing (when the show actually happens)
        val planDateRange = DateRange(entity.planStartDate, entity.planEndDate)
        val priceRange = PriceRange(entity.priceRangeMin, entity.priceRangeMax)

        // Sales timing (when tickets are sold) - this is the sell period
        val sellPeriod = if (entity.sellFrom != null && entity.sellTo != null) {
            DateRange(entity.sellFrom!!, entity.sellTo!!)
        } else null

        val zones = entity.zones.map { zoneEntityMapper.toDomain(it) }
        return Plan.createOnline(
            id = PlanId.of(entity.id),
            title = entity.title,
            date = planDateRange,  // When the plan happens
            priceRange = priceRange,
            providerPlanId = entity.providerPlanId,
            organizerCompanyId = entity.organizerCompanyId,
            sellPeriod = sellPeriod,  // When tickets are sold
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
            planStartDate = domain.date.from,      // Plan timing
            planEndDate = domain.date.to,          // Plan timing
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

    fun toEntityForUpdate(domain: Plan, existingEntity: PlanJpaEntity): PlanJpaEntity {
        return PlanJpaEntity(
            id = existingEntity.id,
            providerPlanId = existingEntity.providerPlanId,
            title = domain.title,
            planStartDate = domain.date.from,      // Plan timing
            planEndDate = domain.date.to,          // Plan timing
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