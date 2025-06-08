package com.jmb.events_api.sync.infrastructure.persistence

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "zones")
class ZoneJpaEntity(
    @Id val zoneId: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    val event: EventJpaEntity
)
