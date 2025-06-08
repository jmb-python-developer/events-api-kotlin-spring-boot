package com.jmb.events_api.sync.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EventJpaRepository: JpaRepository<EventJpaEntity, String> {
    fun findByProviderEventId(eventProviderId: String)
}
