package com.jmb.events_api.sync.application.service


/**
 * Contains the necessary business logic to search for events through ports. Called by Infrastructure classes.
 *
 * Triggering flow:
 *
 * REST Controller (Infrastructure)
 *     → calls SearchEventsService (Application)
 *         → uses EventRepositoryPort (Domain interface)
 *             → implemented by JPA adapter (Infrastructure)
 */
class SearchEventsService(
    //TODO: Add dependent ports soon, when they are done.
) {
    //TODO
}
