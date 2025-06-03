package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime


class Event(
    val id: Long,
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val priceRange: PriceRange,
    val sellMode: SellMode,
    val providerEventId: String,        // base_event_id from XML
    val organizerCompanyId: String?,    // organizer_company_id (optional)
    val sellPeriod: DateRange?,         // sell_from/sell_to dates
    val soldOut: Boolean,
    val lastUpdated: Instant,           // When we last synced an event
    val version: Long = 1               // For optimistic locking
) {
    //Validations for the Event creation
    init {
        require(providerEventId.isNotBlank()) { "Provider event ID cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(startDate.isBefore(endDate)) { "Start date must be before end date" }
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

    fun updateFromProvider(
        newTitle: String,
        newStartDate: LocalDateTime,
        newEndDate: LocalDateTime,
        newPriceRange: PriceRange,
        newSellFrom: LocalDateTime,
        newSellTo: LocalDateTime,
        newSoldOut: Boolean = false,
        newOrganizerCompanyId: String? = null
    ): Event {
        return Event(
            id = this.id,
            providerEventId = this.providerEventId,
            title = newTitle.trim(),
            startDate = newStartDate,
            endDate = newEndDate,
            priceRange = newPriceRange,
            sellMode = this.sellMode,
            organizerCompanyId = newOrganizerCompanyId,
            sellPeriod = DateRange(newSellFrom, newSellTo),
            soldOut = newSoldOut,
            lastUpdated = Instant.now(),
            version = this.version + 1,
        )
    }

    //Online events type factory method
    companion object {

        fun createOnline(
            id: Long,
            title: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            priceRange: PriceRange,
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
                priceRange,
                sellMode = SellMode.ONLINE,
                providerEventId = providerEventId,
                organizerCompanyId = organizerCompanyId,
                sellPeriod = sellPeriod,
                soldOut = soldOut,
                lastUpdated = lastUpdated,
                version = version,
            )
        }

        // Alternative factory for provider sync scenarios
        fun fromProviderData(
            id: Long,
            providerEventId: String,
            title: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            zones: List<Zone> // You'll need to create this class
        ): Event {
            val minPrice = zones.minOfOrNull { it.price } ?: BigDecimal.ZERO
            val maxPrice = zones.maxOfOrNull { it.price } ?: BigDecimal.ZERO
            val priceRange = PriceRange(minPrice, maxPrice)

            return createOnline(
                id = id,
                providerEventId = providerEventId,
                title = title,
                startDate = startDate,
                endDate = endDate,
                priceRange = priceRange,
            )
        }
    }

}

// Zone class for calculating prices
data class Zone(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean
)

enum class SellMode {
    ONLINE,
    OFFLINE
}