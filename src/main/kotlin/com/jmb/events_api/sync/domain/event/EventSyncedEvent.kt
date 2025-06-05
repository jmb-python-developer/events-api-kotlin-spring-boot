package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.EventId
import com.jmb.events_api.sync.domain.model.PriceRange
import java.time.Instant
import java.util.UUID

/**
 * Represents and event concerning a change in the [Event] domain model.
 */
data class EventSyncedEvent(
    val eventEntityId: EventId,
    val title: String,
    val providerEventId: String,
    val organizerCompanyId: String?,
    val eventDate: DateRange,           // Your actual date field
    val priceRange: PriceRange,
    val sellPeriod: DateRange?,
    val soldOut: Boolean,
    val version: Long,
    val syncedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.EVENT_SYNCED.value,
) : DomainEvent
