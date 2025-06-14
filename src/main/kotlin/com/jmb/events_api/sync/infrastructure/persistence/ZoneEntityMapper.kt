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

    fun toEntity(domain: Zone, planId: String, index: Int): ZoneJpaEntity {
        //zones might repeat id but differ in numbered/non_number seats
        val uniqueZoneId = "${planId}-zone-${index}"

        return ZoneJpaEntity(
            zoneId = uniqueZoneId,
            name = domain.name,
            price = domain.price,
            capacity = domain.capacity,
            numbered = domain.numbered,
        )
    }
}
