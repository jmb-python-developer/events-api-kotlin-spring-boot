package com.jmb.events_api.sync.application.dto

data class SyncEventsCommand(
    val forceSync: Boolean = false,
    val batchSize:Int = 100,
)
