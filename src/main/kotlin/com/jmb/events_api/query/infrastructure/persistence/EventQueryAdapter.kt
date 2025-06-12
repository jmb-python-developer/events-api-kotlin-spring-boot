package com.jmb.events_api.query.infrastructure.persistence

import com.jmb.events_api.query.application.dto.EventResponseDto
import com.jmb.events_api.query.application.port.EventQueryPort
import com.jmb.events_api.sync.infrastructure.persistence.EventJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Query adapter implementing optimized read-only operations.
 * Reuses the EventJpaRepository but focuses on query optimization.
 */
@Component
class EventQueryAdapter(
    private val eventJpaRepository: EventJpaRepository,
) : EventQueryPort {

    private val logger = LoggerFactory.getLogger(EventQueryAdapter::class.java)

    override suspend fun findEventsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime,
        availableOnly: Boolean,
        maxResults: Int
    ): List<EventResponseDto> {
        logger.debug("Querying events from {} to {} (availableOnly: {})", startDate, endDate, availableOnly)

        val pageable = PageRequest.of(0, maxResults)

        val entities = if (availableOnly) {
            eventJpaRepository.findAvailableEventsByDateRange(startDate, endDate, pageable)
        } else {
            eventJpaRepository.findEventsByDateRange(startDate, endDate, pageable)
        }

        val events = entities.take(maxResults).map { entity ->
            EventResponseDto.fromDomain(
                eventId = entity.id,
                title = entity.title,
                startDateTime = entity.sellFrom,
                endDateTime = entity.sellTo,
                minPrice = entity.priceRangeMin,
                maxPrice = entity.priceRangeMax
            )
        }
        logger.debug("Found ${events.size} events")
        return events
    }

    override suspend fun findEventById(eventId: String): EventResponseDto? {
        val entity = eventJpaRepository.findById(eventId).orElse(null)
        return entity?.let {
            EventResponseDto.fromDomain(
                eventId = it.id,
                title = it.title,
                startDateTime = it.sellFrom,
                endDateTime = it.sellTo,
                minPrice = it.priceRangeMin,
                maxPrice = it.priceRangeMax
            )
        }
    }

    override suspend fun countEventsByDateRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Long {
        val count = eventJpaRepository.countEventsByDateRange(startDate, endDate)
        logger.debug("Number of events from {} to {}: {} ", startDate, endDate, count)
        return count
    }

    override suspend fun findEventsByPriceRange(
        minPrice: BigDecimal,
        maxPrice: BigDecimal
    ): List<EventResponseDto> {
        val entities = eventJpaRepository.findEventsByPriceRange(minPrice, maxPrice)
        val events = entities.map { entity ->
            EventResponseDto.fromDomain(
                eventId = entity.id,
                title = entity.title,
                startDateTime = entity.sellFrom,
                endDateTime = entity.sellTo,
                minPrice = entity.priceRangeMin,
                maxPrice = entity.priceRangeMax
            )
        }
        logger.debug("Found ${events.size} events within price rante")
        return events
    }
}