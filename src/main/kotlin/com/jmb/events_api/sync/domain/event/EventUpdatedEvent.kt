package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import com.jmb.events_api.sync.domain.model.EventId
import java.time.Instant
import java.util.UUID

data class EventUpdatedEvent(
    val eventEntityId: EventId,
    val providerEventId: String,
    val previousVersion: Long,
    val newVersion: Long,
    // Specific change flags
    val titleChanged: Boolean,
    val priceRangeChanged: Boolean,
    val sellPeriodChanged: Boolean,
    val soldOutStatusChanged: Boolean,
    val organizerCompanyIdChanged: Boolean,
    // Optional: include the actual changes for detailed tracking
    val previousTitle: String?,
    val newTitle: String?,
    val updatedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.EVENT_UPDATED.value,
) : DomainEvent