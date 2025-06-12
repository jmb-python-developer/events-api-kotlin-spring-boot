package com.jmb.events_api.query.application.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal

data class EventResponseDto(
    val id: String,
    val title: String,
    @JsonProperty("start_date") val startDate: String,
    @JsonProperty("start_time") val startTime: String,
    @JsonProperty("end_date") val endDate: String,
    @JsonProperty("end_time") val endTime: String,
    @JsonProperty("min_price") val minPrice: BigDecimal,
    @JsonProperty("max_price") val maxPrice: BigDecimal
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
                startDate = startDateTime.toLocalDate().toString(),
                startTime = startDateTime.toLocalTime().toString(),
                endDate = endDateTime.toLocalDate().toString(),
                endTime = endDateTime.toLocalTime().toString(),
                minPrice = minPrice,
                maxPrice = maxPrice
            )
        }
    }
}
