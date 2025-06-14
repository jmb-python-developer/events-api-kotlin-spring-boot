package com.jmb.events_api.query.infrastructure.persistence

import com.jmb.events_api.query.application.dto.EventResponseDto
import com.jmb.events_api.query.application.port.EventQueryPort
import com.jmb.events_api.sync.infrastructure.persistence.PlanJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Query adapter implementing optimized read-only operations.
 * Reuses the PlanJpaRepository but focuses on query optimization.
 */
@Component
class PlanQueryAdapter(
    private val planJpaRepository: PlanJpaRepository,
) : EventQueryPort {

    private val logger = LoggerFactory.getLogger(PlanQueryAdapter::class.java)

    override suspend fun findEventsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        availableOnly: Boolean,
        maxResults: Int
    ): List<EventResponseDto> {
        logger.debug("Querying plans from {} to {} (availableOnly: {})", startDate, endDate, availableOnly)

        val pageable = PageRequest.of(0, maxResults)

        val entities = if (availableOnly) {
            planJpaRepository.findAvailablePlansByDateRange(startDate, endDate, pageable)
        } else {
            planJpaRepository.findPlansByDateRange(startDate, endDate, pageable)
        }

        val events = entities.take(maxResults).map { entity ->
            EventResponseDto.fromDomain(
                eventId = entity.id,
                title = entity.title,
                startDateTime = entity.planStartDate,  // Use plan timing for display
                endDateTime = entity.planEndDate,      // Use plan timing for display
                minPrice = entity.priceRangeMin,
                maxPrice = entity.priceRangeMax
            )
        }
        logger.debug("Found ${events.size} plans")
        return events
    }

    override suspend fun findEventById(eventId: String): EventResponseDto? {
        val entity = planJpaRepository.findById(eventId).orElse(null)
        return entity?.let {
            EventResponseDto.fromDomain(
                eventId = it.id,
                title = it.title,
                startDateTime = it.planStartDate,  // Use plan timing for display
                endDateTime = it.planEndDate,      // Use plan timing for display
                minPrice = it.priceRangeMin,
                maxPrice = it.priceRangeMax
            )
        }
    }
}