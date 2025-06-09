package com.jmb.events_api.sync.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventJpaRepository: JpaRepository<EventJpaEntity, String> {
    fun findByProviderEventId(eventProviderId: String): EventJpaEntity?

    @Modifying
    @Query("""
        MERGE INTO event (
            id, provider_event_id, title, sell_from, sell_to,
            price_range_min, price_range_max, sell_mode,
            organizer_company_id, sell_period_from, sell_period_to,
            sold_out, last_update, version
        ) VALUES (
            :#{#entity.id}, :#{#entity.providerEventId}, :#{#entity.title},
            :#{#entity.sellFrom}, :#{#entity.sellTo},
            :#{#entity.priceRangeMin}, :#{#entity.priceRangeMax}, :#{#entity.sellMode},
            :#{#entity.organizerCompanyId}, :#{#entity.sellPeriodFrom}, :#{#entity.sellPeriodTo},
            :#{#entity.soldOut}, :#{#entity.lastUpdated}, :#{#entity.version}
        )
    """, nativeQuery = true)
    fun upsertEvent(@Param("entity") entity: EventJpaEntity): Int


    //Named query methods to search based on business functionality as main criteria
    @Query("""
               SELECT e FROM EventJpaEntity e
                WHERE e.sellFrom >= :startDate
                AND e.sellTo <= :endDate
                ORDER BY e.sellFrom ASC 
    """
    )
    fun findEventsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime,
    ): List<EventJpaEntity>

    @Query("""
        SELECT e FROM EventJpaEntity e 
        WHERE e.sellFrom >= :startDate AND e.sellTo <= :endDate
        AND e.soldOut = false
        ORDER BY e.sellFrom ASC
    """)
    fun findAvailableEventsByDateRange(
        @Param("startDate") startDate: LocalDateTime,
        @Param("endDate") endDate: LocalDateTime
    ): List<EventJpaEntity>

    fun findBySellMode(sellMode: String): List<EventJpaEntity>

    fun countByProviderEventId(providerEventId: String): Long
}
