package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import java.time.Instant
import java.util.UUID

data class PlanFailedEvent(
    val providerEventId: String,
    val failureReason: String,
    val failedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.PLAN_SYNC_FAILED.value,
): DomainEvent
