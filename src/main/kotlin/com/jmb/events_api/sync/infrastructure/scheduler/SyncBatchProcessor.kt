package com.jmb.events_api.sync.infrastructure.scheduler

import com.jmb.events_api.sync.application.dto.ProviderPlanDto
import com.jmb.events_api.sync.application.service.SyncPlansService
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.Plan
import com.jmb.events_api.sync.domain.model.PlanId
import com.jmb.events_api.sync.domain.model.Zone
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SyncBatchProcessor(
    private val syncPlansService: SyncPlansService
) {
    private val logger = LoggerFactory.getLogger(SyncBatchProcessor::class.java)

    suspend fun processBatch(
        plans: List<ProviderPlanDto>,
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
            totalPlans = plans.size,                                    // Updated field name
            successfulPlans = batchResults.sumOf { it.successCount },   // Updated field name
            failedPlans = batchResults.sumOf { it.errorCount }          // Updated field name
        )
    }

    private suspend fun processPlanBatch(batch: List<ProviderPlanDto>, batchId: Int): BatchResult {
        val domainPlans = batch.mapNotNull { planDto ->
            try {
                Plan.fromProviderData(
                    id = PlanId.generate(),
                    providerPlanId = planDto.basePlanId,
                    organizerCompanyId = planDto.organizerCompanyId,
                    sellPeriod = DateRange(planDto.sellFrom, planDto.sellTo),
                    soldOut = planDto.soldOut,
                    title = planDto.title,
                    date = DateRange(planDto.planStartDate, planDto.planEndDate),
                    zones = planDto.zones.map { Zone(it.zoneId, it.name, it.price, it.capacity, it.numbered) }
                )
            } catch (e: Exception) {
                logger.error("Failed to map plan ${planDto.basePlanId}", e)
                null
            }
        }

        if (domainPlans.isEmpty()) {
            return BatchResult(batchId, batch.size, 0, batch.size)
        }

        val processedPlans = syncPlansService.syncPlans(plans = domainPlans)
        return BatchResult(
            batchNumber = batchId,
            plansProcessed = batch.size,                    // Updated field name
            successCount = processedPlans.size,
            errorCount = batch.size - processedPlans.size,
        )
    }
}