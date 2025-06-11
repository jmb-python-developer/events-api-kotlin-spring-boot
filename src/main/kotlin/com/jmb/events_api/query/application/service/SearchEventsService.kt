package com.jmb.events_api.query.application.service

import com.jmb.events_api.query.application.dto.SearchEventsQuery
import com.jmb.events_api.query.application.dto.SearchEventsResult
import com.jmb.events_api.query.application.port.EventQueryPort
import com.jmb.events_api.query.application.port.SearchEventsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.system.measureTimeMillis


@Service
class SearchEventsService(
    private val eventQueryPort: EventQueryPort
) : SearchEventsUseCase {

    private val logger = LoggerFactory.getLogger(SearchEventsService::class.java)

    override suspend fun searchEvents(query: SearchEventsQuery): SearchEventsResult = withContext(Dispatchers.IO) {
        logger.info("Executing search query: ${query.startsAt} to ${query.endsAt}")

        var events: List<com.jmb.events_api.query.application.dto.EventResponseDto> = emptyList()

        val executionTime = measureTimeMillis {
            // Convert dates to include full days
            val startDateTime = query.startsAt.atStartOfDay()
            val endDateTime = query.endsAt.atTime(23, 59, 59)

            // Execute search using the port
            events = eventQueryPort.findEventsByDateRange(
                startDate = startDateTime,
                endDate = endDateTime,
                availableOnly = query.availableOnly,
                maxResults = query.maxResults
            )
        }

        logger.info("Found ${events.size} events in ${executionTime}ms")

        // Return complete result with all information
        SearchEventsResult(
            events = events,
            totalCount = events.size,
            searchCriteria = query,
            executionTimeMs = executionTime
        )
    }
}