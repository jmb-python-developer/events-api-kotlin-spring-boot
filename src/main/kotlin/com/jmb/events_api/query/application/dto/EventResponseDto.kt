package com.jmb.events_api.query.application.dto

import java.math.BigDecimal

data class EventResponseDto(
    val id: String,
    val title: String,
    val start_date: String,  // YYYY-MM-DD format
    val start_time: String,  // HH:mm:ss format
    val end_date: String,    // YYYY-MM-DD format
    val end_time: String,    // HH:mm:ss format
    val min_price: BigDecimal,
    val max_price: BigDecimal
) {
    companion object {
        fun fromDomain(
            eventId: String,
            title: String,
            startDateTime: java.time.LocalDateTime,
            endDateTime: java.time.LocalDateTime,
            minPrice: BigDecimal,
            maxPrice: BigDecimal
        ): EventResponseDto {
            return EventResponseDto(
                id = eventId,
                title = title,
                start_date = startDateTime.toLocalDate().toString(),
                start_time = startDateTime.toLocalTime().toString(),
                end_date = endDateTime.toLocalDate().toString(),
                end_time = endDateTime.toLocalTime().toString(),
                min_price = minPrice,
                max_price = maxPrice
            )
        }
    }
}
