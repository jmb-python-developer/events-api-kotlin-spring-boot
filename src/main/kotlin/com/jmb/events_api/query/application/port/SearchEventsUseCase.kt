package com.jmb.events_api.query.application.port

import com.jmb.events_api.query.application.dto.SearchEventsQuery
import com.jmb.events_api.query.application.dto.SearchEventsResult

interface SearchEventsUseCase {
    suspend fun searchEvents(query: SearchEventsQuery): SearchEventsResult
}