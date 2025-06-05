package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.EventId
import com.jmb.events_api.sync.domain.model.PriceRange
import java.time.Instant
import java.util.UUID

data class EventUpdatedEvent(
    val eventEntityId: EventId,
    val providerEventId: String,
    val previousVersion: Long,
    val newVersion: Long,

    // ACTUAL previous and new values (not just flags)
    val previousTitle: String,
    val newTitle: String,
    val previousPriceRange: PriceRange,
    val newPriceRange: PriceRange,
    val previousSellPeriod: DateRange?,
    val newSellPeriod: DateRange?,
    val previousSoldOut: Boolean,
    val newSoldOut: Boolean,
    val previousOrganizerCompanyId: String?,
    val newOrganizerCompanyId: String?,

    val updatedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.EVENT_UPDATED.value,
) : DomainEvent