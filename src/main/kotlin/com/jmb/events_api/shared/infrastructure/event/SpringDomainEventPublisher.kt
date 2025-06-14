package com.jmb.events_api.shared.infrastructure.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Internal Published for the Events happening within the modules of this App. This principle could/should
 * - depending on the architecture evolution based on scaling be extended to use a Broker's EventPublisher
 * in a different service.
 */
@Component
class SpringDomainEventPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
): DomainEventPublisher {
    private val logger = LoggerFactory.getLogger(SpringDomainEventPublisher::class.java)

    override fun publish(event: DomainEvent) {
        logger.debug("Publishing Domain Event: ${event.eventType}")
        applicationEventPublisher.publishEvent(event)
    }

    override fun publish(events: List<DomainEvent>) {
        events.forEach { publish(it) }
    }
}

