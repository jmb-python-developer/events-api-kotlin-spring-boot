package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.Zone
import org.springframework.stereotype.Component

@Component
class ZoneEntityMapper {

    fun toDomain(entity: ZoneJpaEntity): Zone {
        return Zone(
            id = entity.zoneId,
            name = entity.name,
            price = entity.price,
            capacity = entity.capacity,
            numbered = entity.numbered
        )
    }

    fun toEntity(domain: Zone, planEntity: PlanJpaEntity): ZoneJpaEntity {
        return ZoneJpaEntity(
            zoneId = domain.id,
            name = domain.name,
            price = domain.price,
            capacity = domain.capacity,
            numbered = domain.numbered,
        )
    }

    fun toDomainList(entities: List<ZoneJpaEntity>): List<Zone> {
        return entities.map { toDomain(it) }
    }

    fun toEntityList(domains: List<Zone>, planEntity: PlanJpaEntity): List<ZoneJpaEntity> {
        return domains.map { toEntity(it, planEntity) }
    }
}