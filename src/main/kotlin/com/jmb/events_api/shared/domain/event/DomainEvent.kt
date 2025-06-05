package com.jmb.events_api.shared.domain.event

import java.time.Instant

interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val eventType: String
}

enum class EventType(val value: String) {
    EVENT_SYNCED("EventSynced"),
    EVENT_UPDATED("EventUpdated"),
    EVENT_SYNC_FAILED("EventSynced"),
}