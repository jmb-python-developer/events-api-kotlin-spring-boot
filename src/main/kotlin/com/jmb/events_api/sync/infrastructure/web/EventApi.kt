package com.jmb.events_api.sync.infrastructure.web

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.EventSyncedEvent
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.model.EventId
import com.jmb.events_api.sync.domain.model.PriceRange
import com.jmb.events_api.sync.domain.model.Zone
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.PostMapping
import java.math.BigDecimal
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/events")
class EventApi(
    private val domainEventPublisher: DomainEventPublisher
) {

    @GetMapping("/hello")
    fun hello(): String {
        return "Hello from Event API with Domain Events! 🚀"
    }

    @PostMapping("/sync/demo")
    fun syncDemo(): Map<String, Any> {
        val demoZones = listOf(
            Zone(
                id = "zone-1",
                name = "VIP Section",
                price = BigDecimal("75.00"),
                capacity = 50,
                numbered = true
            ),
            Zone(
                id = "zone-2",
                name = "General Admission",
                price = BigDecimal("25.00"),
                capacity = 200,
                numbered = false
            )
        )

        val event = Event.fromProviderData(
            id = EventId.generate(),
            providerEventId = "demo-123",
            title = "Demo Concert with Real Structure",
            date = DateRange(
                sellFrom = LocalDateTime.now().plusDays(30),
                sellTo = LocalDateTime.now().plusDays(30).plusHours(2)
            ),
            zones = demoZones
        )

        // ✅ CLEAN TEST - Create and publish a domain event directly!
        val testEvent = EventSyncedEvent(
            eventEntityId = event.id,
            title = event.title,
            providerEventId = event.providerEventId,
            organizerCompanyId = event.organizerCompanyId,
            eventDate = event.date,
            priceRange = event.priceRange,
            sellPeriod = event.sellPeriod,
            soldOut = event.soldOut,
            version = event.version
        )

        // 🚀 Publish the domain event - this will trigger all handlers!
        domainEventPublisher.publish(testEvent)

        return mapOf(
            "message" to "Demo event synced with REAL domain events!",
            "eventId" to event.id.value,
            "title" to event.title,
            "domainEventPublished" to testEvent.eventType,
            "domainEventId" to testEvent.eventId,
            "note" to "Check logs - event handlers should have responded!"
        )
    }

    @PutMapping("/update/demo/{eventId}")
    fun updateDemo(@PathVariable eventId: String): Map<String, Any> {
        // Create existing event first
        val existingZones = listOf(
            Zone("zone-1", "VIP", BigDecimal("50.00"), 50, true),
            Zone("zone-2", "GA", BigDecimal("30.00"), 200, false)
        )

        val existingEvent = Event.fromProviderData(
            id = EventId.of(eventId), // Assuming EventId.of() exists
            providerEventId = "demo-existing-123",
            title = "Existing Concert",
            date = DateRange(
                sellFrom = LocalDateTime.now().plusDays(15),
                sellTo = LocalDateTime.now().plusDays(15).plusHours(3)
            ),
            zones = existingZones
        )

        // Update using your ACTUAL updateFromProvider method signature
        val updatedEvent = existingEvent.updateFromProvider(
            newTitle = "Updated Concert - Special Edition",
            date = DateRange(
                sellFrom = LocalDateTime.now().plusDays(15),
                sellTo = LocalDateTime.now().plusDays(15).plusHours(4) // Extended
            ),
            newPriceRange = PriceRange(
                min = BigDecimal("35.00"), // Assuming PriceRange constructor
                max = BigDecimal("90.00")
            ),
            newSellFrom = LocalDateTime.now().minusDays(3),
            newSellTo = LocalDateTime.now().plusDays(14),
            newSoldOut = false,
            newOrganizerCompanyId = "organizer-999"
        )

        return mapOf(
            "message" to "Event updated with real structure!",
            "eventId" to eventId,
            "previousVersion" to existingEvent.version,
            "newVersion" to updatedEvent.version,
            "changes" to mapOf(
                "titleChanged" to (existingEvent.title != updatedEvent.title),
                "priceChanged" to (existingEvent.priceRange != updatedEvent.priceRange),
                "organizerChanged" to (existingEvent.organizerCompanyId != updatedEvent.organizerCompanyId),
                "previousTitle" to existingEvent.title,
                "newTitle" to updatedEvent.title
            )
        )
    }
}