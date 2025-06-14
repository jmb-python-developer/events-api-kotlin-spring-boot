package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.PlanId
import com.jmb.events_api.sync.domain.model.PriceRange
import java.time.Instant
import java.util.UUID

/**
 * Represents an event concerning a change in the [Plan] domain model.
 */
data class PlanSyncedEvent(
    val planEntityId: PlanId,
    val title: String,
    val providerPlanId: String,
    val organizerCompanyId: String?,
    val planDate: DateRange,           // Your actual date field
    val priceRange: PriceRange,
    val sellPeriod: DateRange?,
    val soldOut: Boolean,
    val version: Long,
    val syncedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.PLAN_SYNCED.value,
) : DomainEvent
