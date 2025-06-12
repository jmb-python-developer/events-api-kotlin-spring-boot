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
    val providerEventId: String,
    val title: String,
    @Column(name = "sell_from")
    val sellFrom: LocalDateTime,
    @Column(name = "sell_to")
    val sellTo: LocalDateTime,
    @Column(name = "price_range_min")
    val priceRangeMin: BigDecimal,
    @Column(name = "price_range_max")
    val priceRangeMax: BigDecimal,
    @Column(name = "sell_mode")
    val sellMode: String,
    @Column(name = "organizer_company_id", nullable = true)
    val organizerCompanyId: String?,
    @Column(name = "sell_period_from", nullable = true)
    val sellPeriodFrom: LocalDateTime?,
    @Column(name = "sell_period_to", nullable = true)
    val sellPeriodTo: LocalDateTime?,
    @Column(name = "sold_out")
    val soldOut: Boolean,
    @Column(name = "last_update")
    val lastUpdated: Instant,
    @OneToMany(mappedBy = "event", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    var zones: List<ZoneJpaEntity> = emptyList(),
    @Version
    val version: Long = 1
)
