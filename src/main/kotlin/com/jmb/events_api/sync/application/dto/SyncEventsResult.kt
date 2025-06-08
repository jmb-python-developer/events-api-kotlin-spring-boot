package com.jmb.events_api.sync.application.dto

import com.jmb.events_api.sync.domain.model.Event

data class SyncEventsResult(
    val events: Collection<Event>,
    val successCount: Int,
    val errorCount: Int,
    val totalProcessed: Int,
)
