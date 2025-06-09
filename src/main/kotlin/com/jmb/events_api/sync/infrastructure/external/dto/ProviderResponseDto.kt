package com.jmb.events_api.sync.infrastructure.external.dto

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.jmb.events_api.sync.application.dto.ProviderEventDto
import com.jmb.events_api.sync.application.dto.ZoneDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JacksonXmlRootElement(localName = "eventList")
data class EventListResponseDto(
    @JacksonXmlProperty(localName = "output")
    val output: OutputDto?
) {
    // Extension function for direct conversion with business logic
    fun toCleanEvents(): List<ProviderEventDto> =
        output?.baseEvents
            ?.filter { it.sellMode.equals("online", ignoreCase = true) }
            ?.map { it.toCleanDto() }
            ?: emptyList()
}

data class OutputDto(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "base_event")
    val baseEvents: List<BaseEventDto> = emptyList()
)


data class EventDetailsDto(
    @JacksonXmlProperty(isAttribute = true, localName = "event_start_date")
    val eventStartDate: String,

    @JacksonXmlProperty(isAttribute = true, localName = "event_end_date")
    val eventEndDate: String,

    @JacksonXmlProperty(isAttribute = true, localName = "sell_from")
    val sellFrom: String,

    @JacksonXmlProperty(isAttribute = true, localName = "sell_to")
    val sellTo: String,

    @JacksonXmlProperty(isAttribute = true, localName = "sold_out")
    val soldOut: String,

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "zone")
    val zones: List<ZoneRawDto> = emptyList()
)


// Extension Functions Approach - Add these to the raw DTOs
data class BaseEventDto(
    @JacksonXmlProperty(isAttribute = true, localName = "base_event_id")
    val baseEventId: String,

    @JacksonXmlProperty(isAttribute = true, localName = "sell_mode")
    val sellMode: String,

    @JacksonXmlProperty(isAttribute = true, localName = "title")
    val title: String,

    @JacksonXmlProperty(isAttribute = true, localName = "organizer_company_id")
    val organizerCompanyId: String?,

    @JacksonXmlProperty(localName = "event")
    val event: EventDetailsDto
) {
    companion object {
        private val PROVIDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    // Extension function for direct conversion
    fun toCleanDto(): ProviderEventDto {
        return ProviderEventDto(
            baseEventId = baseEventId,
            title = title,
            sellMode = sellMode,
            organizerCompanyId = organizerCompanyId,
            eventStartDate = LocalDateTime.parse(event.eventStartDate, PROVIDER_DATE_FORMAT),
            eventEndDate = LocalDateTime.parse(event.eventEndDate, PROVIDER_DATE_FORMAT),
            sellFrom = LocalDateTime.parse(event.sellFrom, PROVIDER_DATE_FORMAT),
            sellTo = LocalDateTime.parse(event.sellTo, PROVIDER_DATE_FORMAT),
            soldOut = event.soldOut.toBoolean(),
            zones = event.zones.map { it.toCleanDto() }
        )
    }
}

// Extension for zones
data class ZoneRawDto(
    @JacksonXmlProperty(isAttribute = true, localName = "zone_id")
    val zoneId: String,

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    val name: String,

    @JacksonXmlProperty(isAttribute = true, localName = "price")
    val price: String,

    @JacksonXmlProperty(isAttribute = true, localName = "capacity")
    val capacity: String,

    @JacksonXmlProperty(isAttribute = true, localName = "numbered")
    val numbered: String
) {
    fun toCleanDto(): ZoneDto {
        return ZoneDto(
            zoneId = zoneId,
            name = name,
            price = BigDecimal(price),
            capacity = capacity.toInt(),
            numbered = numbered.toBoolean()
        )
    }
}