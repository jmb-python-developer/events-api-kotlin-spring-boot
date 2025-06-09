package com.jmb.events_api.sync.infrastructure.external

import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class ProviderApiClient(
    private val restTemplate: RestTemplate,
    private val providerEventMapper: ProviderEventMapper,
) {

}
