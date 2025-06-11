package com.jmb.events_api.query.application.dto

import java.time.LocalDate

/**
 * Query object representing the search criteria for events.
 * Maps to GET /search?starts_at=...&ends_at=... parameters
 */
data class SearchEventsQuery(
    val startsAt: LocalDate,
    val endsAt: LocalDate,
    val availableOnly: Boolean = false, // Optional: filter only non-sold-out events
    val maxResults: Int = 1000 // Optional: pagination support
) {
    init {
        require(!startsAt.isAfter(endsAt)) {
            "Start date cannot be after end date"
        }
    }
}
