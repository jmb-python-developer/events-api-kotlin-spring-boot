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
        events: List<ProviderEventDto>,
        batchSize: Int = 50,
    ): BatchProcessingResult {
        val batchResults = events.chunked(batchSize)
            .withIndex()
            .map { (batchId, batch) ->
                try {
                    processEventBatch(batch, batchId)
                } catch (ex: Exception) {
                    logger.error("Error processing batch $batchId", ex)
                    BatchResult(batchId, batch.size, 0, batch.size)
                }
            }
        return BatchProcessingResult(
            totalBatches = batchResults.size,
            successfulBatches = batchResults.filter { it.errorCount != 0 }.size,
            failedBatches = batchResults.filter { it.errorCount == 0 }.size,
            totalEvents = events.size,
            successfulEvents = batchResults.sumOf { it.successCount },
            failedEvents = batchResults.sumOf { it.errorCount }
        )
    }

    private suspend fun processEventBatch(batch: List<ProviderEventDto>, batchId: Int): BatchResult {
        val domainEvents = batch.map { eventDto ->
            Event.fromProviderData(
                id = EventId.generate(),
                providerEventId = eventDto.baseEventId,
                organizerCompanyId = eventDto.organizerCompanyId,
                sellPeriod = DateRange(eventDto.sellFrom, eventDto.sellTo),
                soldOut = eventDto.soldOut,
                title = eventDto.title,
                date = DateRange(eventDto.eventStartDate, eventDto.eventEndDate),
                zones = eventDto.zones.map { Zone(it.zoneId, it.name, it.price, it.capacity, it.numbered) }
            )
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