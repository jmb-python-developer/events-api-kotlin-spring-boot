package com.jmb.events_api.query.application.dto

/**
 * Response wrapper for search operations containing the events list
 * and metadata about the search operation
 */
data class SearchEventsResult(
    val events: List<EventResponseDto>,
    val totalCount: Int,
    val searchCriteria: SearchEventsQuery,
    val executionTimeMs: Long = 0
) {
    companion object {
        fun empty(query: SearchEventsQuery): SearchEventsResult {
            return SearchEventsResult(
                events = emptyList(),
                totalCount = 0,
                searchCriteria = query
            )
        }

        fun success(
            events: List<EventResponseDto>,
            query: SearchEventsQuery,
            executionTime: Long = 0
        ): SearchEventsResult {
            return SearchEventsResult(
                events = events,
                totalCount = events.size,
                searchCriteria = query,
                executionTimeMs = executionTime
            )
        }
    }
}
