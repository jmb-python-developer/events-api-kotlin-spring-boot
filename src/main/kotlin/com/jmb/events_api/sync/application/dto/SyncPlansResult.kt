package com.jmb.events_api.sync.application.dto

import com.jmb.events_api.sync.domain.model.Plan

data class SyncPlansResult(
    val plans: Collection<Plan>,      // Updated from events
    val successCount: Int,
    val errorCount: Int,
    val totalProcessed: Int,
)