package com.jmb.events_api.sync.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProviderEventDto(
    val baseEventId: String,
    val title: String,
    val sellMode: String,
    val organizerCompanyId: String?,
    val eventStartDate: LocalDateTime,
    val eventEndDate: LocalDateTime,
    val sellFrom: LocalDateTime,
    val sellTo: LocalDateTime,
    val soldOut: Boolean,
    val zones: List<ZoneDto>
)

data class ZoneDto(
    val zoneId: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean
)
