package com.jmb.events_api.query.infrastructure.web

import com.jmb.events_api.query.application.dto.SearchEventsQuery
import com.jmb.events_api.query.application.port.SearchEventsUseCase
import com.jmb.events_api.query.infrastructure.web.dto.ApiResponse
import com.jmb.events_api.query.infrastructure.web.validation.SearchValidation
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Main API endpoint implementing the Fever challenge requirement:
 * GET /search?starts_at=YYYY-MM-DD&ends_at=YYYY-MM-DD
 */
@RestController
@RequestMapping("/search")
class EventSearchController(
    private val searchEventsUseCase: SearchEventsUseCase,
    private val searchValidation: SearchValidation
) {

    private val logger = LoggerFactory.getLogger(EventSearchController::class.java)

    @GetMapping
    fun searchEvents(
        @RequestParam("starts_at") startsAt: String,
        @RequestParam("ends_at") endsAt: String,
        @RequestParam(defaultValue = "false") availableOnly: Boolean
    ): ResponseEntity<ApiResponse> = runBlocking {

        logger.info("Search request: starts_at=$startsAt, ends_at=$endsAt, availableOnly=$availableOnly")

        return@runBlocking try {
            // Validate input parameters
            val validationResult = searchValidation.validateSearchParams(startsAt, endsAt)
            if (!validationResult.isValid) {
                return@runBlocking ResponseEntity.badRequest()
                    .body(ApiResponse.error("VALIDATION_ERROR", validationResult.errorMessage!!))
            }

            // Create query object
            val query = SearchEventsQuery(
                startsAt = LocalDate.parse(startsAt),
                endsAt = LocalDate.parse(endsAt),
                availableOnly = availableOnly
            )

            // Execute search
            val result = searchEventsUseCase.searchEvents(query)

            // Return success response
            ResponseEntity.ok(ApiResponse.success(result.events))

        } catch (e: Exception) {
            logger.error("Search failed", e)
            val (code, message) = when (e) {
                is IllegalArgumentException -> "INVALID_INPUT" to e.message
                is DateTimeParseException -> "INVALID_DATE_FORMAT" to "Invalid date format"
                is DataAccessException -> "DATABASE_ERROR" to "Database temporarily unavailable"
                else -> "INTERNAL_ERROR" to "Search operation failed"
            }
            ResponseEntity.internalServerError()
                .body(ApiResponse.error(code, message ?: "Unknown error"))
        }
    }
}