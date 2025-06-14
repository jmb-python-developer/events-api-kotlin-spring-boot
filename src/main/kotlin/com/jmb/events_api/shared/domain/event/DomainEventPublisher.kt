package com.jmb.events_api.shared.domain.event

interface DomainEventPublisher {
    fun publish(event: DomainEvent)
    fun publish(events: List<DomainEvent>)
}
