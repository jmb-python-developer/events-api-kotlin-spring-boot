package com.jmb.events_api.sync.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration

@Configuration
@EnableScheduling
class SchedulingConfig {

    @ConfigurationProperties(prefix = "fever.sync")
    data class SyncJobProperties(
        val interval: Duration = Duration.ofSeconds(5),
        val batchSize: Int = 100,           // Plans per batch
        val maxRetries: Int = 3,            // Max retries for plan sync
        val enabled: Boolean = true,        // Enable/disable plan sync
        val healthCheckInterval: Duration = Duration.ofMinutes(5)
    )
}
