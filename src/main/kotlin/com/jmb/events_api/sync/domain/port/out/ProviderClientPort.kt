package com.jmb.events_api.sync.domain.port.out

import com.jmb.events_api.sync.application.dto.ProviderPlanDto
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Port interface for fetching plan data from external provider
 */
interface ProviderClientPort {
    suspend fun fetchPlans(): List<ProviderPlanDto>
}

//Default values added as well as fallback from application.yml and diff profiles
@ConfigurationProperties(prefix = "fever.provider")
data class ProviderProperties(
    val url: String = "https://provider.code-challenge.feverup.com/api/events", // Provider still calls it events
    val timeout: Duration = Duration.ofSeconds(5),
    val retryAttempts: Int = 3,
    val retryDelay: Duration = Duration.ofMillis(500)
)
