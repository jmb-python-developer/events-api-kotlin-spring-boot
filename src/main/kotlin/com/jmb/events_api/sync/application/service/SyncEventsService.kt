package com.jmb.events_api.sync.application.service

import com.jmb.events_api.sync.domain.model.Event

/**
 * Contains business logic to sync events by calling the necessary ports. Called by Infrastructure objects.
 *
 * Triggering flow:
 *
 * ScheduledJob (Infrastructure)
 *     → calls SyncEventsService (Application)
 *         → uses EventRepositoryPort + ProviderClientPort (Domain interfaces)
 *             → implemented by JPA + HTTP adapters (Infrastructure)
 */
class SyncEventsService(
    //TODO: Add dependent ports soon, when they are done.
) {
    fun syncEvents(events: Collection<Event>): Collection<Event> {
        //TODO: Implement this after ports are done.
        return emptyList()
    }
}
