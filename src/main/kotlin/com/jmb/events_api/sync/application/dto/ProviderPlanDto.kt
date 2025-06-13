package com.jmb.events_api.sync.application.dto

import java.math.BigDecimal
import java.time.LocalDateTime

data class ProviderPlanDto(
    val basePlanId: String,           // Updated from baseEventId
    val title: String,
    val sellMode: String,
    val organizerCompanyId: String?,
    val planStartDate: LocalDateTime, // Updated from eventStartDate
    val planEndDate: LocalDateTime,   // Updated from eventEndDate
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