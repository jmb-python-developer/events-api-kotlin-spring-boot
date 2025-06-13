package com.jmb.events_api.sync.infrastructure.persistence

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "zone")
class ZoneJpaEntity(
    @Id
    val zoneId: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean
) {
    // Simple event relationship - SEPARATE FROM CONSTRUCTOR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    lateinit var event: EventJpaEntity

    // JPA no-arg constructor
    constructor() : this("", "", BigDecimal.ZERO, 0, false)
}
