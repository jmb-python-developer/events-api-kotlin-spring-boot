package com.jmb.events_api.sync.infrastructure.external

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.jmb.events_api.sync.application.dto.ProviderEventDto
import com.jmb.events_api.sync.domain.port.out.ProviderClientPort
import com.jmb.events_api.sync.domain.port.out.ProviderProperties
import com.jmb.events_api.sync.infrastructure.external.dto.EventListResponseDto
import com.jmb.events_api.sync.infrastructure.external.exception.ProviderApiException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ProviderApiClient(
    private val restTemplate: RestTemplate,
    private val providerProperties: ProviderProperties,
    private val xmlMapper: XmlMapper,
) : ProviderClientPort {

    private val logger = LoggerFactory.getLogger(ProviderApiClient::class.java)

    override suspend fun fetchEvents(): List<ProviderEventDto> {
        return try {
            logger.info("Fetching events from provider: ${providerProperties.url}")
            val xmlContent = restTemplate.getForObject(providerProperties.url, String::class.java)
            val eventListResponse = xmlMapper.readValue(xmlContent, EventListResponseDto::class.java)
            eventListResponse.toCleanEvents() // Extension function does everything
        } catch (e: Exception) {
            logger.error("Failed to fetch events from provider", e)
            throw ProviderApiException.networkError(e)
        }
    }

}
