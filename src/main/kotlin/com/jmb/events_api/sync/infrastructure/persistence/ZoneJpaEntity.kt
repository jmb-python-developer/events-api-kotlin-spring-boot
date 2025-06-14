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
    // Simple plan relationship - SEPARATE FROM CONSTRUCTOR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    lateinit var plan: PlanJpaEntity

    // To provide Spring JPA "no-arg" constructor required by library
    constructor() : this("", "", BigDecimal.ZERO, 0, false)
}
