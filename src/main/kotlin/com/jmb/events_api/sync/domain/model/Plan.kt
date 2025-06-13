package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

class Plan(
    val id: PlanId,
    val title: String,
    val date: DateRange,
    val priceRange: PriceRange,
    val sellMode: SellMode,
    val providerPlanId: String,        // base_plan_id from XML
    val organizerCompanyId: String?,    // organizer_company_id (optional)
    val sellPeriod: DateRange?,         // sell_from/sell_to dates
    val soldOut: Boolean,
    val lastUpdated: Instant,           // When we last synced a plan
    val zones: List<Zone>,
    val version: Long = 1               // For optimistic locking
) {
    //Validations for the Plan creation
    init {
        require(providerPlanId.isNotBlank()) { "Provider plan ID cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(sellMode == SellMode.ONLINE) { "Only online plans are allowed" }
    }

    fun isCurrentlyOnSale(): Boolean {
        val now = Instant.now()
        return !soldOut && sellPeriod?.let {
            DateRange.isInPeriodRange(now, it.from, it.to)
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
    ): Plan {
        return Plan(
            id = this.id,
            providerPlanId = this.providerPlanId,
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

    //Online plans type factory method
    companion object {

        fun createOnline(
            id: PlanId,
            title: String,
            date: DateRange,
            priceRange: PriceRange,
            providerPlanId: String,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false,
            lastUpdated: Instant = Instant.now(),
            zones: List<Zone>,
            version: Long = 1,
        ): Plan {
            return Plan(
                id = id,
                title = title,
                date = date,
                priceRange,
                sellMode = SellMode.ONLINE,
                providerPlanId = providerPlanId,
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
            id: PlanId,
            providerPlanId: String,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false,
            title: String,
            date: DateRange,
            zones: List<Zone>,
        ): Plan {
            val priceRange = if (zones.isEmpty()) {
                throw IllegalArgumentException("Plan must have at least one zone")
            } else {
                val minPrice = zones.minOf { it.price }
                val maxPrice = zones.maxOf { it.price }
                PriceRange(minPrice, maxPrice)
            }

            return createOnline(
                id = id,
                providerPlanId = providerPlanId,
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