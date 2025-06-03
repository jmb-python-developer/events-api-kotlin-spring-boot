package com.jmb.events_api.sync.domain.model

import java.math.BigDecimal
import java.time.LocalDateTime


data class Event(
    val id: Long,
    val title: String,
    val startDate: LocalDateTime,
    val endDate: LocalDateTime,
    val minPrice: BigDecimal,
    val maxPrice: BigDecimal,
    val sellMode: SellMode,
) {
    //Validations for the Event
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
        require(startDate.isBefore(endDate)) { "Start date must be before end date" }
        require(minPrice >= BigDecimal.ZERO) { "Min price cannot be negative" }
        require(maxPrice >= BigDecimal.ZERO) { "Max price cannot be negative" }
        require(minPrice <= maxPrice) { "Min price cannot be greater than max price" }
        require(sellMode == SellMode.ONLINE) { "Only online events are allowed" }
    }

    //Online events type factory method
    companion object {
        fun createOnline(
            id: Long,
            title: String,
            startDate: LocalDateTime,
            endDate: LocalDateTime,
            minPrice: BigDecimal,
            maxPrice: BigDecimal
        ): Event {
            return Event(
                id = id,
                title = title,
                startDate = startDate,
                endDate = endDate,
                minPrice = minPrice,
                maxPrice = maxPrice,
                sellMode = SellMode.ONLINE
            )
        }
    }

}

enum class SellMode  {
    ONLINE,
    OFFLINE
}