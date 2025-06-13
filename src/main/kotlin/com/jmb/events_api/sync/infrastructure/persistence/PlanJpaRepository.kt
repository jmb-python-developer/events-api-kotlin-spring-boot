package com.jmb.events_api.sync.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PlanJpaRepository : JpaRepository<PlanJpaEntity, String> {
    fun findByProviderPlanId(planProviderId: String): PlanJpaEntity?

    //Named query methods to search based on business functionality as main criteria
    @Query(
        """
               SELECT p FROM PlanJpaEntity p
                WHERE p.planStartDate >= :startDate
                AND p.planEndDate <= :endDate
                ORDER BY p.planStartDate ASC 
    """
    )
    fun findPlansByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable,
    ): List<PlanJpaEntity>

    @Query(
        """
        SELECT p FROM PlanJpaEntity p 
        WHERE p.planStartDate >= :startDate AND p.planEndDate <= :endDate
        AND p.soldOut = false
        ORDER BY p.planStartDate ASC
    """
    )
    fun findAvailablePlansByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
        pageable: Pageable
    ): List<PlanJpaEntity>

    @Query(
        """
            SELECT COUNT(p.id) FROM PlanJpaEntity p
            WHERE p.planStartDate >= :startDate AND p.planEndDate <= :endDate
            AND p.soldOut = false
        """
    )
    fun countPlansByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): Long

    @Query(
        """
        SELECT p FROM PlanJpaEntity p
        WHERE p.priceRangeMin >= :minPrice AND p.priceRangeMax <= :maxPrice
        ORDER BY p.priceRangeMin ASC
        """
    )
    fun findPlansByPriceRange(
        @Param("minPrice") minPrice: java.math.BigDecimal,
        @Param("maxPrice") maxPrice: java.math.BigDecimal
    ): List<PlanJpaEntity>

    fun findBySellMode(sellMode: String): List<PlanJpaEntity>

    fun countByProviderPlanId(providerPlanId: String): Long
}