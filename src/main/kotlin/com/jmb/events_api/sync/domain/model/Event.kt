package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime


class Event(
    val id: Long,
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal,
    val sellMode: SellMode,

    val providerEventId: String,        // base_event_id from XML
    val organizerCompanyId: String?,    // organizer_company_id (optional)
    val sellPeriod: DateRange?,         // sell_from/sell_to dates
    val soldOut: Boolean,

    // Domain properties
    val lastUpdated: Instant,           // When we last synced an event
    val version: Long = 1               // For optimistic locking
) {
    //Validations for the Event creation
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(startDate.isBefore(endDate)) { "Start date must be before end date" }
        require(minPrice >= BigDecimal.ZERO) { "Min price cannot be negative" }
        require(maxPrice >= BigDecimal.ZERO) { "Max price cannot be negative" }
        require(minPrice <= maxPrice) { "Min price cannot be greater than max price" }
        require(sellMode == SellMode.ONLINE) { "Only online events are allowed" }
    }

    fun isCurrentlyOnSale(): Boolean {
        val now = Instant.now()
        return !soldOut && sellPeriod?.let {
            DateRange.isInPeriodRange(now, it.sellFrom, it.sellTo)
        } == true
    }

    fun isHappeningInDateRange(searchRange: DateRange): Boolean {
        return sellPeriod?.let {
            searchRange.overlapsWith(sellPeriod)
        } ?: false
    }

    //Online events type factory method
    companion object {
        fun createOnline(
            id: Long,
            title: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            minPrice: BigDecimal,
            maxPrice: BigDecimal,
            providerEventId: String,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false,
            lastUpdated: Instant = Instant.now(),
            version: Long = 1,
        ): Event {
            return Event(
                id = id,
                title = title,
                startDate = startDate,
                endDate = endDate,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sellMode = SellMode.ONLINE,
                providerEventId = providerEventId,
                organizerCompanyId = organizerCompanyId,
                sellPeriod = sellPeriod,
                soldOut = soldOut,
                lastUpdated = lastUpdated,
                version = version,
            )
        }
    }

}

enum class SellMode  {
    ONLINE,
    OFFLINE
}