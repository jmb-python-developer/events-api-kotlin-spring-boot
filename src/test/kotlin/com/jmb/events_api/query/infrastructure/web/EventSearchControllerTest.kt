package com.jmb.events_api.query.infrastructure.web

import com.jmb.events_api.query.application.dto.EventResponseDto
import com.jmb.events_api.query.application.dto.SearchEventsQuery
import com.jmb.events_api.query.application.dto.SearchEventsResult
import com.jmb.events_api.query.application.port.SearchEventsUseCase
import com.jmb.events_api.query.infrastructure.web.validation.SearchValidation
import com.jmb.events_api.query.infrastructure.web.validation.ValidationResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.dao.DataAccessResourceFailureException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

@WebMvcTest(EventSearchController::class)
class EventSearchControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var searchEventsUseCase: SearchEventsUseCase

    @MockBean
    private lateinit var searchValidation: SearchValidation

    @Test
    fun `should return events successfully`() {
        val events = listOf(createEventDto("1", "Concert A"))
        val result = SearchEventsResult(
            events = events,
            totalCount = 1,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30)
            ),
            executionTimeMs = 100
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error").isEmpty)
            .andExpect(jsonPath("$.data.events").isArray)
            .andExpect(jsonPath("$.data.events[0].id").value("1"))
            .andExpect(jsonPath("$.data.events[0].title").value("Concert A"))
    }

    @Test
    fun `should return empty events list`() {
        val result = SearchEventsResult(
            events = emptyList(),
            totalCount = 0,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30)
            )
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error").isEmpty)
            .andExpect(jsonPath("$.data.events").isEmpty)
    }

    @Test
    fun `should handle availableOnly parameter`() {
        val result = SearchEventsResult(
            events = emptyList(),
            totalCount = 0,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30),
                availableOnly = true
            )
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
                .param("availableOnly", "true")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error").isEmpty)
    }

    @Test
    fun `should default availableOnly to false`() {
        val result = SearchEventsResult(
            events = emptyList(),
            totalCount = 0,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30),
                availableOnly = false
            )
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should return validation error for invalid dates`() {
        given(
            searchValidation.validateSearchParams("invalid-date", "2021-06-30")
        ).willReturn(ValidationResult.invalid("INVALID_DATE_FORMAT", "Invalid date format"))

        mockMvc.perform(
            get("/search")
                .param("starts_at", "invalid-date")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.message").value("Invalid date format"))
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    fun `should return validation error for date range issues`() {
        given(
            searchValidation.validateSearchParams("2021-06-30", "2021-06-01")
        ).willReturn(ValidationResult.invalid("INVALID_DATE_RANGE", "starts_at cannot be after ends_at"))

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-30")
                .param("ends_at", "2021-06-01")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.error.message").value("starts_at cannot be after ends_at"))
    }

    @Test
    fun `should handle missing required parameters`() {
        mockMvc.perform(get("/search"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"))
    }

    @Test
    fun `should handle missing starts_at parameter`() {
        mockMvc.perform(
            get("/search")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"))
            .andExpect(jsonPath("$.error.message").value("starts_at parameter is required"))
    }

    @Test
    fun `should handle missing ends_at parameter`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error.code").value("MISSING_PARAMETER"))
            .andExpect(jsonPath("$.error.message").value("ends_at parameter is required"))
    }

    @Test
    fun `should handle IllegalArgumentException from query validation`() {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(query)).willThrow(IllegalArgumentException("Invalid query"))
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
            .andExpect(jsonPath("$.error.message").value("Invalid query"))
    }

    @Test
    fun `should handle DateTimeParseException`() {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(query)).willThrow(DateTimeParseException("Parse error", "invalid", 0))
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("INVALID_DATE_FORMAT"))
            .andExpect(jsonPath("$.error.message").value("Invalid date format"))
    }

    @Test
    fun `should handle DataAccessException`() {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(query)).willThrow(DataAccessResourceFailureException("Database error"))
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("DATABASE_ERROR"))
            .andExpect(jsonPath("$.error.message").value("Database temporarily unavailable"))
    }

    @Test
    fun `should handle generic exceptions`() {
        val query = SearchEventsQuery(
            startsAt = LocalDate.of(2021, 6, 1),
            endsAt = LocalDate.of(2021, 6, 30)
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(query)).willThrow(RuntimeException("Unexpected error"))
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.error.message").value("Search operation failed"))
    }

    @Test
    fun `should handle multiple events in response`() {
        val events = listOf(
            createEventDto("1", "Concert A"),
            createEventDto("2", "Concert B"),
            createEventDto("3", "Festival C")
        )
        val result = SearchEventsResult(
            events = events,
            totalCount = 3,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30)
            )
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.events").isArray)
            .andExpect(jsonPath("$.data.events.length()").value(3))
            .andExpect(jsonPath("$.data.events[0].title").value("Concert A"))
            .andExpect(jsonPath("$.data.events[1].title").value("Concert B"))
            .andExpect(jsonPath("$.data.events[2].title").value("Festival C"))
    }

    @Test
    fun `should validate response format matches API specification`() {
        val events = listOf(createEventDto("1", "Concert"))
        val result = SearchEventsResult(
            events = events,
            totalCount = 1,
            searchCriteria = SearchEventsQuery(
                startsAt = LocalDate.of(2021, 6, 1),
                endsAt = LocalDate.of(2021, 6, 30)
            )
        )

        given(searchValidation.validateSearchParams("2021-06-01", "2021-06-30")).willReturn(ValidationResult.valid())
        runBlocking {
            given(searchEventsUseCase.searchEvents(result.searchCriteria)).willReturn(result)
        }

        mockMvc.perform(
            get("/search")
                .param("starts_at", "2021-06-01")
                .param("ends_at", "2021-06-30")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.error").isEmpty)
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.events").isArray)
            .andExpect(jsonPath("$.data.events[0].id").exists())
            .andExpect(jsonPath("$.data.events[0].title").exists())
            .andExpect(jsonPath("$.data.events[0].start_date").exists())
            .andExpect(jsonPath("$.data.events[0].start_time").exists())
            .andExpect(jsonPath("$.data.events[0].end_date").exists())
            .andExpect(jsonPath("$.data.events[0].end_time").exists())
            .andExpect(jsonPath("$.data.events[0].min_price").exists())
            .andExpect(jsonPath("$.data.events[0].max_price").exists())
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