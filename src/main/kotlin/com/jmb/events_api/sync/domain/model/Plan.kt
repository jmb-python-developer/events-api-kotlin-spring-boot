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
    val providerPlanId: String,
    val organizerCompanyId: String?,
    val sellPeriod: DateRange?,
    val soldOut: Boolean,
    val lastUpdated: Instant,
    val zones: List<Zone>,
    val version: Long = 1
) {
    init {
        require(providerPlanId.isNotBlank()) { "Provider plan ID cannot be blank" }
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(sellMode == SellMode.ONLINE) { "Only online plans are allowed" }
    }

    fun isCurrentlyOnSale(): Boolean =
        !soldOut && sellPeriod?.let { DateRange.isInPeriodRange(Instant.now(), it.from, it.to) } == true

    fun isHappeningInDateRange(searchRange: DateRange): Boolean =
        sellPeriod?.let { searchRange.overlapsWith(it) } ?: false

    fun updateFromProvider(
        newTitle: String,
        date: DateRange,
        newPriceRange: PriceRange,
        newSellFrom: LocalDateTime,
        newSellTo: LocalDateTime,
        newSoldOut: Boolean = false,
        newOrganizerCompanyId: String? = null,
        newZones: List<Zone>
    ): Plan = Plan(
        id = id,
        providerPlanId = providerPlanId,
        title = newTitle.trim(),
        date = date,
        priceRange = newPriceRange,
        sellMode = sellMode,
        organizerCompanyId = newOrganizerCompanyId,
        sellPeriod = DateRange(newSellFrom, newSellTo),
        soldOut = newSoldOut,
        lastUpdated = Instant.now(),
        zones = newZones,
        version = version + 1
    )

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
            version: Long = 1
        ): Plan = Plan(
            id = id,
            title = title,
            date = date,
            priceRange = priceRange,
            sellMode = SellMode.ONLINE,
            providerPlanId = providerPlanId,
            organizerCompanyId = organizerCompanyId,
            sellPeriod = sellPeriod,
            soldOut = soldOut,
            lastUpdated = lastUpdated,
            zones = zones,
            version = version
        )

        fun fromProviderData(
            id: PlanId,
            providerPlanId: String,
            title: String,
            date: DateRange,
            zones: List<Zone>,
            organizerCompanyId: String? = null,
            sellPeriod: DateRange? = null,
            soldOut: Boolean = false
        ): Plan {
            require(zones.isNotEmpty()) { "Plan must have at least one zone" }

            val minPrice = zones.minOf { it.price }
            val maxPrice = zones.maxOf { it.price }

            return createOnline(
                id = id,
                providerPlanId = providerPlanId,
                title = title,
                date = date,
                priceRange = PriceRange(minPrice, maxPrice),
                zones = zones,
                organizerCompanyId = organizerCompanyId,
                sellPeriod = sellPeriod,
                soldOut = soldOut
            )
        }

        fun Plan.detectChangesComparedTo(other: Plan): List<String> = buildList {
            if (title != other.title) add("title: '$title' → '${other.title}'")
            if (!priceRange.isEquivalentTo(other.priceRange)) add("price: ${priceRange.min}-${priceRange.max} → ${other.priceRange.min}-${other.priceRange.max}")
            if (!date.isEquivalentTo(other.date)) add("date: ${date.from} to ${date.to} → ${other.date.from} to ${other.date.to}")
            if (soldOut != other.soldOut) add("soldOut: $soldOut → ${other.soldOut}")
            if (organizerCompanyId != other.organizerCompanyId) add("organizer: '$organizerCompanyId' → '${other.organizerCompanyId}'")
            if (!sellPeriodsEquivalent(sellPeriod, other.sellPeriod)) add("sellPeriod: $sellPeriod → ${other.sellPeriod}")
        }

        private fun sellPeriodsEquivalent(p1: DateRange?, p2: DateRange?): Boolean =
            p1 == p2 || (p1 != null && p2 != null && p1.isEquivalentTo(p2))

    }
}

data class Zone(
    val id: String,
    val name: String,
    val price: BigDecimal,
    val capacity: Int,
    val numbered: Boolean
)

enum class SellMode {
    ONLINE, OFFLINE
}
