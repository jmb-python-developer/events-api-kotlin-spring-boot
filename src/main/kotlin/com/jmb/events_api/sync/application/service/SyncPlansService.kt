package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.PlanSyncedEvent
import com.jmb.events_api.sync.domain.event.PlanUpdatedEvent
import com.jmb.events_api.sync.domain.event.PlanFailedEvent
import com.jmb.events_api.sync.domain.model.Plan
import com.jmb.events_api.sync.domain.model.Plan.Companion.detectChangesComparedTo
import com.jmb.events_api.sync.domain.repository.PlanRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.joinToString
import kotlin.collections.mapNotNull

@Service
@Transactional
class SyncPlansService(
    private val planRepository: PlanRepository,
    private val domainEventPublisher: DomainEventPublisher
) {

    private val logger = LoggerFactory.getLogger(SyncPlansService::class.java)

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
    }

    suspend fun syncPlans(plans: Collection<Plan>): Collection<Plan> {
        return plans.mapNotNull { plan ->
            try {
                processPlan(plan)
            } catch (e: OptimisticLockingFailureException) {
                // After max retries exhausted, handle as failure
                logger.error("Plan ${plan.providerPlanId} failed after ${MAX_RETRY_ATTEMPTS} retry attempts", e)
                publishFailureEvent(plan, e)
                null
            } catch (e: Exception) {
                // Handle other non-retryable exceptions
                logger.error("Failed to process plan ${plan.providerPlanId}", e)
                publishFailureEvent(plan, e)
                null
            }
        }
    }

    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    private suspend fun processPlan(plan: Plan): Plan {
        logger.debug("Processing plan: ${plan.providerPlanId}")

        return planRepository.findByProviderId(plan.providerPlanId)?.let { existing ->
            updateIfChanged(existing, plan)
        } ?: createNewPlan(plan)
    }

    private suspend fun createNewPlan(plan: Plan): Plan {
        val savedPlan = planRepository.upsertPlan(plan)
        domainEventPublisher.publish(savedPlan.toSyncedEvent())
        logger.debug("Created new plan: {} ({})", savedPlan.title, savedPlan.providerPlanId)
        return savedPlan
    }

    private suspend fun updateIfChanged(existing: Plan, incoming: Plan): Plan {
        val detectedChanges = existing.detectChangesComparedTo(incoming)
        if (detectedChanges.isEmpty()) {
            logger.debug("No significant changes for plan: ${incoming.title}")
            return existing
        }

        logger.info("Changes for '${incoming.title}' (${incoming.providerPlanId}): ${detectedChanges.joinToString()}")

        val updatedPlan = existing.updateFromProvider(
            newTitle = incoming.title,
            date = incoming.date,
            newPriceRange = incoming.priceRange,
            newSellFrom = incoming.sellPeriod?.from ?: incoming.date.from,
            newSellTo = incoming.sellPeriod?.to ?: incoming.date.to,
            newSoldOut = incoming.soldOut,
            newOrganizerCompanyId = incoming.organizerCompanyId,
            newZones = incoming.zones,
        )

        val savedPlan = planRepository.upsertPlan(updatedPlan)
        domainEventPublisher.publish(savedPlan.toUpdatedEvent(existing))
        logger.debug("Updated plan: ${savedPlan.title} (${savedPlan.providerPlanId})")
        return savedPlan
    }

    private fun publishFailureEvent(plan: Plan, exception: Exception) {
        domainEventPublisher.publish(
            PlanFailedEvent(
                providerEventId = plan.providerPlanId,
                failureReason = exception.message ?: "Unknown error"
            )
        )
    }

    private fun Plan.toSyncedEvent() = PlanSyncedEvent(
        planEntityId = id,
        title = title,
        providerPlanId = providerPlanId,
        organizerCompanyId = organizerCompanyId,
        planDate = date,
        priceRange = priceRange,
        sellPeriod = sellPeriod,
        soldOut = soldOut,
        version = version
    )

    private fun Plan.toUpdatedEvent(previous: Plan) = PlanUpdatedEvent(
        planEntityId = id,
        providerPlanId = providerPlanId,
        previousVersion = previous.version,
        newVersion = version,
        previousTitle = previous.title,
        newTitle = title,
        previousPriceRange = previous.priceRange,
        newPriceRange = priceRange,
        previousSellPeriod = previous.sellPeriod,
        newSellPeriod = sellPeriod,
        previousSoldOut = previous.soldOut,
        newSoldOut = soldOut,
        previousOrganizerCompanyId = previous.organizerCompanyId,
        newOrganizerCompanyId = organizerCompanyId
    )
}