package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(EventRepositoryAdapter::class, EventEntityMapper::class, ZoneEntityMapper::class)
class EventRepositoryAdapterIntegrationTest {

    @Autowired
    private lateinit var eventRepositoryAdapter: EventRepositoryAdapter

    @Test
    fun `should save and retrieve event with proper mapping`() {
        // Given
        val eventId = EventId.generate()
        val dateRange = DateRange(
            sellFrom = LocalDateTime.now().plusDays(1),
            sellTo = LocalDateTime.now().plusDays(1).plusHours(2)
        )
        val priceRange = PriceRange(BigDecimal("25.00"), BigDecimal("75.00"))

        val event = Event.createOnline(
            id = eventId,
            title = "Test Concert",
            date = dateRange,
            priceRange = priceRange,
            providerEventId = "TEST-123",
            organizerCompanyId = "ORG-456"
        )

        // When
        val savedEvent = eventRepositoryAdapter.upsertEvent(event)
        val retrievedEvent = eventRepositoryAdapter.findById(savedEvent.id.value)

        // Then
        assertNotNull(retrievedEvent)
        assertEquals("Test Concert", retrievedEvent.title)
        assertEquals("TEST-123", retrievedEvent.providerEventId)
        assertEquals(BigDecimal("25.00"), retrievedEvent.priceRange.min)
        assertEquals(BigDecimal("75.00"), retrievedEvent.priceRange.max)
        assertEquals("ORG-456", retrievedEvent.organizerCompanyId)
        assertEquals(1L, retrievedEvent.version)
    }

    @Test
    fun `should return null when event not found by id`() {
        // Given
        val nonExistentId = "non-existent-id-123"

        // When
        val result = eventRepositoryAdapter.findById(nonExistentId)

        // Then
        assertNull(result)
    }

    @Test
    fun `should return null when event not found by provider id`() {
        // Given
        val nonExistentProviderId = "non-existent-provider-123"

        // When
        val result = eventRepositoryAdapter.findByProviderId(nonExistentProviderId)

        // Then
        assertNull(result)
    }

    @Test
    fun `should find event by provider id`() {
        // Given
        val event = createTestEvent("PROVIDER-123", "Concert by Provider ID")
        eventRepositoryAdapter.upsertEvent(event)

        // When
        val foundEvent = eventRepositoryAdapter.findByProviderId("PROVIDER-123")

        // Then
        assertNotNull(foundEvent)
        assertEquals("Concert by Provider ID", foundEvent.title)
        assertEquals("PROVIDER-123", foundEvent.providerEventId)
    }

    @Test
    fun `should update existing event when saving with same provider id`() {
        // Given - Save initial event
        val originalEvent = createTestEvent("UPDATE-TEST-123", "Original Title")
        val savedEvent = eventRepositoryAdapter.upsertEvent(originalEvent)

        // When - Update with same provider ID but different title
        val updatedEvent = savedEvent.updateFromProvider(
            newTitle = "Updated Title",
            date = savedEvent.date,
            newPriceRange = PriceRange(BigDecimal("50.00"), BigDecimal("100.00")),
            newSellFrom = savedEvent.sellPeriod!!.sellFrom,
            newSellTo = savedEvent.sellPeriod!!.sellTo,
            newSoldOut = true,
            newOrganizerCompanyId = "NEW-ORG-789"
        )

        val finalEvent = eventRepositoryAdapter.upsertEvent(updatedEvent)

        // Then
        assertEquals(savedEvent.id.value, finalEvent.id.value) // Same ID
        assertEquals("Updated Title", finalEvent.title)
        assertEquals(BigDecimal("50.00"), finalEvent.priceRange.min)
        assertEquals(BigDecimal("100.00"), finalEvent.priceRange.max)
        assertTrue(finalEvent.soldOut)
        assertEquals("NEW-ORG-789", finalEvent.organizerCompanyId)
        assertEquals(2L, finalEvent.version) // Version incremented

        // Verify only one record exists
        val countCheck = eventRepositoryAdapter.count()
        assertEquals(1L, countCheck)
    }

    @Test
    fun `should handle events with null optional fields`() {
        // Given
        val event = Event.createOnline(
            id = EventId.generate(),
            title = "Event with Nulls",
            date = DateRange(
                sellFrom = LocalDateTime.now().plusDays(1),
                sellTo = LocalDateTime.now().plusDays(1).plusHours(1)
            ),
            priceRange = PriceRange(BigDecimal("10.00"), BigDecimal("10.00")),
            providerEventId = "NULL-TEST-456",
            organizerCompanyId = null, // Null organizer
            sellPeriod = null, // Null sell period
            soldOut = false
        )

        // When
        val savedEvent = eventRepositoryAdapter.upsertEvent(event)
        val retrievedEvent = eventRepositoryAdapter.findById(savedEvent.id.value)

        // Then
        assertNotNull(retrievedEvent)
        assertEquals("Event with Nulls", retrievedEvent.title)
        assertNull(retrievedEvent.organizerCompanyId)
        assertNull(retrievedEvent.sellPeriod)
        assertEquals(false, retrievedEvent.soldOut)
    }

    @Test
    fun `should handle multiple events with different provider ids`() {
        // Given
        val event1 = createTestEvent("MULTI-1", "First Event")
        val event2 = createTestEvent("MULTI-2", "Second Event")
        val event3 = createTestEvent("MULTI-3", "Third Event")

        // When
        eventRepositoryAdapter.upsertEvent(event1)
        eventRepositoryAdapter.upsertEvent(event2)
        eventRepositoryAdapter.upsertEvent(event3)

        // Then
        val foundEvent1 = eventRepositoryAdapter.findByProviderId("MULTI-1")
        val foundEvent2 = eventRepositoryAdapter.findByProviderId("MULTI-2")
        val foundEvent3 = eventRepositoryAdapter.findByProviderId("MULTI-3")

        assertNotNull(foundEvent1)
        assertNotNull(foundEvent2)
        assertNotNull(foundEvent3)

        assertEquals("First Event", foundEvent1.title)
        assertEquals("Second Event", foundEvent2.title)
        assertEquals("Third Event", foundEvent3.title)

        val totalCount = eventRepositoryAdapter.count()
        assertEquals(3L, totalCount)

        println("✅ Multiple events test passed!")
    }

    @Test
    fun `should preserve timestamps and versions correctly`() {
        // Given
        val beforeSave = Instant.now()
        val event = createTestEvent("TIMESTAMP-TEST", "Timestamp Event")

        // When
        val savedEvent = eventRepositoryAdapter.upsertEvent(event)
        val afterSave = Instant.now()

        // Then
        assertTrue(savedEvent.lastUpdated.isAfter(beforeSave) || savedEvent.lastUpdated == beforeSave)
        assertTrue(savedEvent.lastUpdated.isBefore(afterSave) || savedEvent.lastUpdated == afterSave)
        assertEquals(1L, savedEvent.version)

        println("✅ Timestamps and versions test passed!")
    }

    // Helper method to create test events
    private fun createTestEvent(providerId: String, title: String): Event {
        return Event.createOnline(
            id = EventId.generate(),
            title = title,
            date = DateRange(
                sellFrom = LocalDateTime.now().plusDays(1),
                sellTo = LocalDateTime.now().plusDays(1).plusHours(2)
            ),
            priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("80.00")),
            providerEventId = providerId,
            organizerCompanyId = "TEST-ORG",
            sellPeriod = DateRange(
                sellFrom = LocalDateTime.now().minusDays(1),
                sellTo = LocalDateTime.now().plusDays(10)
            ),
            soldOut = false
        )
    }
}