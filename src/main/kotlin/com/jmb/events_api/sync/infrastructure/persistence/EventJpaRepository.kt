package com.jmb.events_api.sync.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventJpaRepository : JpaRepository<EventJpaEntity, String> {
    fun findByProviderEventId(eventProviderId: String): EventJpaEntity?

    //Named query methods to search based on business functionality as main criteria
    @Query(
        """
               SELECT e FROM EventJpaEntity e
                WHERE e.planStartDate >= :startDate
                AND e.planEndDate <= :endDate
                ORDER BY e.planStartDate ASC 
    """
    )
    fun findEventsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable,
    ): List<EventJpaEntity>

    @Query(
        """
        SELECT e FROM EventJpaEntity e 
        WHERE e.planStartDate >= :startDate AND e.planEndDate <= :endDate
        AND e.soldOut = false
        ORDER BY e.planStartDate ASC
    """
    )
    fun findAvailableEventsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): List<EventJpaEntity>

    @Query(
        """
            SELECT COUNT(e.id) FROM EventJpaEntity e
            WHERE e.planStartDate >= :startDate AND e.planEndDate <= :endDate
            AND e.soldOut = false
        """
    )
    fun countEventsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    @Query(
        """
        SELECT e FROM EventJpaEntity e
        WHERE e.priceRangeMin >= :minPrice AND e.priceRangeMax <= :maxPrice
        ORDER BY e.priceRangeMin ASC
        """
    )
    fun findEventsByPriceRange(
        @Param("minPrice") minPrice: java.math.BigDecimal,
        @Param("maxPrice") maxPrice: java.math.BigDecimal
    ): List<EventJpaEntity>

    fun findBySellMode(sellMode: String): List<EventJpaEntity>

    fun countByProviderEventId(providerEventId: String): Long
}