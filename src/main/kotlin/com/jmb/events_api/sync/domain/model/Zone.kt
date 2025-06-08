package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal

/**
 * Represents an Event's Zone.
 */
data class Zone(
    val zoneId: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean,
)
