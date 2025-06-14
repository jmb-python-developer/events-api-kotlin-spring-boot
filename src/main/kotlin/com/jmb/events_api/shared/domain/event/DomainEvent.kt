package com.jmb.events_api.shared.domain.event

import java.time.Instant

interface DomainEvent {
    val eventId: String
    val occurredAt: Instant
    val eventType: String
}

enum class EventType(val value: String) {
    PLAN_SYNCED("PlanSynced"),
    PLAN_UPDATED("PlanUpdated"),
    PLAN_SYNC_FAILED("PlanSyncFailed"),
}
