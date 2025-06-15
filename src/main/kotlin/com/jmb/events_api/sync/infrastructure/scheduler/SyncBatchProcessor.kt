

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
        if (plans.isEmpty()) {
            return BatchProcessingResult.empty()
        }

        val batchResults = plans.chunked(batchSize)
            .withIndex()
            .map { (batchId, batch) ->
                processPlanBatch(batch, batchId)
            }

        return BatchProcessingResult.fromBatchResults(batchResults, plans.size)
    }

    private suspend fun processPlanBatch(batch: List<ProviderPlanDto>, batchId: Int): BatchResult {
        logger.debug("Processing batch $batchId with ${batch.size} plans")

        var mappingFailures = 0
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
                logger.error("Failed to map plan ${planDto.basePlanId} to domain object", e)
                mappingFailures++
                null
            }
        }

        var serviceFailures = 0
        val processedPlans = if (domainPlans.isNotEmpty()) {
            try {
                syncPlansService.syncPlans(domainPlans)
            } catch (e: Exception) {
                logger.error("Service failed to process batch $batchId", e)
                serviceFailures = domainPlans.size
                emptyList()
            }
        } else {
            emptyList()
        }

        val totalFailures = mappingFailures + serviceFailures
        val totalSuccesses = processedPlans.size

        return BatchResult(
            batchNumber = batchId,
            plansProcessed = batch.size,
            successCount = totalSuccesses,
            errorCount = totalFailures,
            mappingFailures = mappingFailures,
            serviceFailures = serviceFailures
        )
    }
}