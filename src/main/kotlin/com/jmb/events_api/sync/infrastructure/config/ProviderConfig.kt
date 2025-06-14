package com.jmb.events_api.sync.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.jmb.events_api.sync.domain.port.out.ProviderProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties

@Configuration
@EnableConfigurationProperties(ProviderProperties::class)
class ProviderConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    /**
     * XML mapper configured for parsing plan data from provider API
     */
    fun xmlMapper(): XmlMapper {
        return XmlMapper().apply {
            // Don't fail on unknown XML properties (plan structure may evolve)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // Handle empty strings as null
            configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
            // Be lenient with dates
            configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
            // Register Kotlin module for proper data class support
            registerModule(KotlinModule.Builder().build())
        }
    }
}
