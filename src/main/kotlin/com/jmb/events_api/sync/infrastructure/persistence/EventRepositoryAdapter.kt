package com.jmb.events_api.sync.infrastructure.persistence

import org.springframework.stereotype.Component

@Component
class EventRepositoryAdapter(
    private val eventJpaRepository: EventJpaRepository,
    private val eventEntityMapper: EventEntityMapper,
) {
}
