package com.jmb.events_api.sync.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.scheduling.annotation.EnableScheduling
import java.time.Duration

@EnableScheduling
class SchedulingConfig {

    @ConfigurationProperties(prefix = "fever.sync")
    data class SyncJobProperties(
        val interval: Duration = Duration.ofSeconds(5),
        val batchSize: Int = 100,
        val maxRetries: Int = 3,
        val enabled: Boolean = true,
        val healthCheckInterval: Duration = Duration.ofMinutes(5)
    )
}