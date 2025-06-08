package com.jmb.events_api.sync.domain.repository

import com.jmb.events_api.sync.domain.model.Event

interface EventRepository {
    fun saveEvent(event: Event): Event
    fun findById(eventId: String): Event?
    fun findByProviderId(providerId: String): Event?
}