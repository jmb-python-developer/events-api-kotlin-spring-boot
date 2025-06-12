package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

class Event(
    val id: EventId,
    val title: String,
    val date: DateRange,
    val priceRange: PriceRange,
    val sellMode: SellMode,
    val providerEventId: String,        // base_event_id from XML
    val organizerCompanyId: String?,    // organizer_company_id (optional)
    val sellPeriod: DateRange?,         // sell_from/sell_to dates
    val soldOut: Boolean,
    val lastUpdated: Instant,           // When we last synced an event
    val zones: List<Zone>,
    val version: Long = 1               // For optimistic locking
) {
    //Validations for the Event creation
    init {
        require(providerEventId.isNotBlank()) { "Provider event ID cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
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
        date: DateRange,
        newPriceRange: PriceRange,
        newSellFrom: LocalDateTime,
        newSellTo: LocalDateTime,
        newSoldOut: Boolean = false,
        newOrganizerCompanyId: String? = null,
        newZones: List<Zone>,
    ): Event {
        return Event(
            id = this.id,
            providerEventId = this.providerEventId,
            title = newTitle.trim(),
            date = date,
            priceRange = newPriceRange,
            sellMode = this.sellMode,
            organizerCompanyId = newOrganizerCompanyId,
            sellPeriod = DateRange(newSellFrom, newSellTo),
            soldOut = newSoldOut,
            lastUpdated = Instant.now(),
            zones = newZones,
            version = this.version + 1,
        )
    }

    //Online events type factory method
    companion object {

        fun createOnline(
            id: EventId,
            title: String,
            date: DateRange,
            priceRange: PriceRange,
            providerEventId: String,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false,
            lastUpdated: Instant = Instant.now(),
            zones: List<Zone>,
            version: Long = 1,
        ): Event {
            return Event(
                id = id,
                title = title,
                date = date,
                priceRange,
                sellMode = SellMode.ONLINE,
                providerEventId = providerEventId,
                organizerCompanyId = organizerCompanyId,
                sellPeriod = sellPeriod,
                soldOut = soldOut,
                lastUpdated = lastUpdated,
                zones = zones,
                version = version,
            )
        }

        // Alternative factory for provider sync scenarios
        fun fromProviderData(
            id: EventId,
            providerEventId: String,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false,
            title: String,
            date: DateRange,
            zones: List<Zone>,
        ): Event {
            val minPrice = zones.minOfOrNull { it.price } ?: BigDecimal.ZERO
            val maxPrice = zones.maxOfOrNull { it.price } ?: BigDecimal.ZERO
            val priceRange = PriceRange(minPrice, maxPrice)

            return createOnline(
                id = id,
                providerEventId = providerEventId,
                organizerCompanyId = organizerCompanyId,
                sellPeriod = sellPeriod,
                soldOut = soldOut,
                title = title,
                date = date,
                priceRange = priceRange,
                zones = zones,
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