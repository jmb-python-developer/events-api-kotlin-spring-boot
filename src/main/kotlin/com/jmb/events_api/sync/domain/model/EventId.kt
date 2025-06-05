package com.jmb.events_api.sync.domain.model

import java.util.UUID

@JvmInline
value class EventId(val value: String) {
    companion object {
        fun generate(): EventId = EventId(UUID.randomUUID().toString())
        fun of(value: String): EventId = EventId(value)
    }
}
