package com.jmb.events_api.query.application.port

import com.jmb.events_api.query.application.dto.EventResponseDto
import java.time.LocalDateTime

/**
 * Port interface for querying events.
 * Optimized for read operations - separate from the sync module's write operations.
 */
interface EventQueryPort {
    suspend fun findEventsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        availableOnly: Boolean = false,
        maxResults: Int = 1000
    ): List<EventResponseDto>

    suspend fun findEventById(eventId: String): EventResponseDto?
}