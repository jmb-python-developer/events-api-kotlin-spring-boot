package com.jmb.events_api.sync.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class ProviderConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
