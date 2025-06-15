package com.jmb.events_api.query.application.service

import com.jmb.events_api.query.application.dto.EventResponseDto
import com.jmb.events_api.query.application.dto.SearchEventsQuery
import com.jmb.events_api.query.application.port.EventQueryPort
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class SearchEventsServiceTest {

    private lateinit var eventQueryPort: EventQueryPort
    private lateinit var searchEventsService: SearchEventsService

    @BeforeEach
    fun setUp() {
        eventQueryPort = mockk()
        searchEventsService = SearchEventsService(eventQueryPort)
    }

    @Test
    fun `should search events successfully`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )
        val expectedEvents = listOf(
            createEventDto("1", "Concert A"),
            createEventDto("2", "Concert B")
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), false, 1000)
        } returns expectedEvents

        val result = searchEventsService.searchEvents(query)

        assertEquals(2, result.events.size)
        assertEquals(2, result.totalCount)
        assertEquals(query, result.searchCriteria)
        assertTrue(result.executionTimeMs >= 0)
        assertEquals("Concert A", result.events[0].title)
        assertEquals("Concert B", result.events[1].title)
    }

    @Test
    fun `should convert dates to full day range`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 15),
            endsAt = LocalDate.of(2021, 6, 20)
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), any())
        } returns emptyList()

        searchEventsService.searchEvents(query)

        coVerify {
            eventQueryPort.findEventsByDateRange(
                LocalDateTime.of(2021, 6, 15, 0, 0, 0),
                LocalDateTime.of(2021, 6, 20, 23, 59, 59),
                false,
                1000
            )
        }
    }

    @Test
    fun `should handle empty results`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 1, 1),
            endsAt = LocalDate.of(2021, 1, 31)
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), any())
        } returns emptyList()

        val result = searchEventsService.searchEvents(query)

        assertEquals(0, result.events.size)
        assertEquals(0, result.totalCount)
        assertEquals(query, result.searchCriteria)
        assertTrue(result.executionTimeMs >= 0)
    }

    @Test
    fun `should handle availableOnly parameter`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30),
            availableOnly = true
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), true, any())
        } returns listOf(createEventDto("1", "Available Event"))

        val result = searchEventsService.searchEvents(query)

        assertEquals(1, result.events.size)
        coVerify {
            eventQueryPort.findEventsByDateRange(any(), any(), true, 1000)
        }
    }

    @Test
    fun `should handle maxResults parameter`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30),
            maxResults = 50
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), 50)
        } returns emptyList()

        searchEventsService.searchEvents(query)

        coVerify {
            eventQueryPort.findEventsByDateRange(any(), any(), false, 50)
        }
    }

    @Test
    fun `should propagate exceptions from port`() {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), any())
        } throws RuntimeException("Database error")

        assertThrows<RuntimeException> {
            runBlocking {
                searchEventsService.searchEvents(query)
            }
        }
    }

    @Test
    fun `should measure execution time`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), any())
        } coAnswers {
            kotlinx.coroutines.delay(10)
            listOf(createEventDto("1", "Event"))
        }

        val result = searchEventsService.searchEvents(query)

        assertTrue(result.executionTimeMs >= 10)
    }

    @Test
    fun `should handle large result sets`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 1, 1),
            endsAt = LocalDate.of(2021, 12, 31),
            maxResults = 5000
        )
        val largeEventList = (1..1000).map { createEventDto(it.toString(), "Event $it") }

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), 5000)
        } returns largeEventList

        val result = searchEventsService.searchEvents(query)

        assertEquals(1000, result.events.size)
        assertEquals(1000, result.totalCount)
    }

    @Test
    fun `should handle date edge cases`() = runBlocking {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 2, 28),
            endsAt = LocalDate.of(2021, 3, 1)
        )

        coEvery {
            eventQueryPort.findEventsByDateRange(any(), any(), any(), any())
        } returns emptyList()

        searchEventsService.searchEvents(query)

        coVerify {
            eventQueryPort.findEventsByDateRange(
                LocalDateTime.of(2021, 2, 28, 0, 0, 0),
                LocalDateTime.of(2021, 3, 1, 23, 59, 59),
                false,
                1000
            )
        }
    }

    private fun createEventDto(id: String, title: String) = EventResponseDto(
        id = id,
        title = title,
        startDate = "2021-06-15",
        startTime = "20:00:00",
        endDate = "2021-06-15",
        endTime = "22:00:00",
        minPrice = BigDecimal("25.00"),
        maxPrice = BigDecimal("75.00")
    )
}