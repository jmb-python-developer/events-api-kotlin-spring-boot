package com.jmb.events_api.sync.infrastructure.persistence

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "EVENT")
class EventJpaEntity(
    @Id
    val id: String,
    @Column(name = "provider_event_id")
    val providerEventId: String,  // Maps to base_plan_id from provider
    val title: String,
    @Column(name = "plan_start_date")
    val planStartDate: LocalDateTime,  // When the actual event/show happens
    @Column(name = "plan_end_date")
    val planEndDate: LocalDateTime,    // When the actual event/show ends
    @Column(name = "price_range_min")
    val priceRangeMin: BigDecimal,
    @Column(name = "price_range_max")
    val priceRangeMax: BigDecimal,
    @Column(name = "sell_mode")
    val sellMode: String,
    @Column(name = "organizer_company_id", nullable = true)
    val organizerCompanyId: String?,
    @Column(name = "sell_from", nullable = true)
    val sellFrom: LocalDateTime?,      // When ticket sales start
    @Column(name = "sell_to", nullable = true)
    val sellTo: LocalDateTime?,        // When ticket sales end
    @Column(name = "sold_out")
    val soldOut: Boolean,
    @Column(name = "last_update")
    val lastUpdated: Instant,
    @Version
    val version: Long = 1
) {
    // Simple zones relationship - NO CONSTRUCTOR PARAMETER
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var zones: MutableList<ZoneJpaEntity> = mutableListOf()

    // JPA no-arg constructor
    constructor() : this("", "", "", LocalDateTime.now(), LocalDateTime.now(),
        BigDecimal.ZERO, BigDecimal.ZERO, "", null, null, null, false, Instant.now(), 1)
}