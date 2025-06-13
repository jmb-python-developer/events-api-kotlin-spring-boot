package com.jmb.events_api.sync.infrastructure.scheduler

import com.jmb.events_api.sync.application.dto.ProviderEventDto
import com.jmb.events_api.sync.application.service.SyncEventsService
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.Event
import com.jmb.events_api.sync.domain.model.EventId
import com.jmb.events_api.sync.domain.model.Zone
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncBatchProcessor(
    private val syncEventsService: SyncEventsService
) {
    private val logger = LoggerFactory.getLogger(SyncBatchProcessor::class.java)

    suspend fun processBatch(
        plans: List<ProviderEventDto>, // Parameter name updated but type stays same
        batchSize: Int = 50,
    ): BatchProcessingResult {
        val batchResults = plans.chunked(batchSize)
            .withIndex()
            .map { (batchId, batch) ->
                try {
                    processPlanBatch(batch, batchId)
                } catch (ex: Exception) {
                    logger.error("Error processing plan batch $batchId", ex)
                    BatchResult(batchId, batch.size, 0, batch.size)
                }
            }
        return BatchProcessingResult(
            totalBatches = batchResults.size,
            successfulBatches = batchResults.filter { it.errorCount == 0 }.size,
            failedBatches = batchResults.filter { it.errorCount != 0 }.size,
            totalEvents = plans.size,
            successfulEvents = batchResults.sumOf { it.successCount },
            failedEvents = batchResults.sumOf { it.errorCount }
        )
    }

    private suspend fun processPlanBatch(batch: List<ProviderEventDto>, batchId: Int): BatchResult {
        val domainEvents = batch.mapNotNull { planDto ->
            try {
                Event.fromProviderData(
                    id = EventId.generate(),
                    providerEventId = planDto.baseEventId, // This maps from base_plan_id
                    organizerCompanyId = planDto.organizerCompanyId,
                    sellPeriod = DateRange(planDto.sellFrom, planDto.sellTo),
                    soldOut = planDto.soldOut,
                    title = planDto.title,
                    date = DateRange(planDto.eventStartDate, planDto.eventEndDate),
                    zones = planDto.zones.map { Zone(it.zoneId, it.name, it.price, it.capacity, it.numbered) }
                )
            } catch (e: Exception) {
                logger.error("Failed to map plan ${planDto.baseEventId}", e)
                null
            }
        }

        if (domainEvents.isEmpty()) {
            return BatchResult(batchId, batch.size, 0, batch.size)
        }

        val processedEvents = syncEventsService.syncEvents(events = domainEvents)
        return BatchResult(
            batchNumber = batchId,
            eventsProcessed = batch.size,
            successCount = processedEvents.size,
            errorCount = batch.size - processedEvents.size,
        )
    }
}