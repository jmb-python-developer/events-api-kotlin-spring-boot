package com.jmb.events_api.sync.infrastructure.external.dto

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.jmb.events_api.sync.application.dto.ProviderPlanDto
import com.jmb.events_api.sync.application.dto.ZoneDto
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@JacksonXmlRootElement(localName = "planList")
data class PlanListResponseDto(
    @JacksonXmlProperty(localName = "output")
    val output: OutputDto?
) {
    // Extension function for direct conversion with business logic
    fun toCleanPlans(): List<ProviderPlanDto> =
        output?.basePlans
            ?.filter { it.sellMode.equals("online", ignoreCase = true) }
            ?.map { it.toCleanDto() }
            ?: emptyList()
}

data class OutputDto(
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "base_plan")
    val basePlans: List<BasePlanDto> = emptyList()
)

data class PlanDetailsDto(
    @JacksonXmlProperty(isAttribute = true, localName = "plan_start_date")
    val planStartDate: String,

    @JacksonXmlProperty(isAttribute = true, localName = "plan_end_date")
    val planEndDate: String,

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
data class BasePlanDto(
    @JacksonXmlProperty(isAttribute = true, localName = "base_plan_id")
    val basePlanId: String,

    @JacksonXmlProperty(isAttribute = true, localName = "sell_mode")
    val sellMode: String,

    @JacksonXmlProperty(isAttribute = true, localName = "title")
    val title: String,

    @JacksonXmlProperty(isAttribute = true, localName = "organizer_company_id")
    val organizerCompanyId: String?,

    @JacksonXmlProperty(localName = "plan")
    val plan: PlanDetailsDto
) {
    companion object {
        private val PROVIDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    }

    // Extension function for direct conversion
    fun toCleanDto(): ProviderPlanDto {
        return ProviderPlanDto(
            basePlanId = basePlanId, // Map base_plan_id to baseEventId
            title = title,
            sellMode = sellMode,
            organizerCompanyId = organizerCompanyId,
            planStartDate = LocalDateTime.parse(plan.planStartDate, PROVIDER_DATE_FORMAT),
            planEndDate = LocalDateTime.parse(plan.planEndDate, PROVIDER_DATE_FORMAT),
            sellFrom = LocalDateTime.parse(plan.sellFrom, PROVIDER_DATE_FORMAT),
            sellTo = LocalDateTime.parse(plan.sellTo, PROVIDER_DATE_FORMAT),
            soldOut = plan.soldOut.toBoolean(),
            zones = plan.zones.map { it.toCleanDto() }
        )
    }
}

// Extension for zones (unchanged)
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
