package com.jmb.events_api.sync.domain.model

import java.util.UUID

@JvmInline
value class PlanId(val value: String) {
    companion object {
        fun generate(): PlanId = PlanId(UUID.randomUUID().toString())
        fun of(value: String): PlanId = PlanId(value)
    }
}
