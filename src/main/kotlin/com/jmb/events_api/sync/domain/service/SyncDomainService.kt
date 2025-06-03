package com.jmb.events_api.sync.domain.service

import com.jmb.events_api.sync.domain.model.Event

interface SyncDomainService {
    fun syncEvents(): Collection<Event>
}