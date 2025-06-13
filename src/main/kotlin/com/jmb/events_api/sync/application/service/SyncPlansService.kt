package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.PlanSyncedEvent
import com.jmb.events_api.sync.domain.event.PlanUpdatedEvent
import com.jmb.events_api.sync.domain.event.PlanFailedEvent
import com.jmb.events_api.sync.domain.model.DateRange
import com.jmb.events_api.sync.domain.model.Plan
import com.jmb.events_api.sync.domain.repository.PlanRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Contains business logic to sync plans by calling the necessary ports. Called by Infrastructure objects.
 *
 * Triggering flow:
 *
 * ScheduledJob (Infrastructure)
 *     → calls SyncPlansService (Application)
 *         → uses PlanRepositoryPort + ProviderClientPort (Domain interfaces)
 *             → implemented by JPA + HTTP adapters (Infrastructure)
 */
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
        return plans.mapNotNull { plan -> processPlan(plan) }
    }

    /**
     * Process single plan with optimistic locking and retry mechanism
     * Called by SyncBatchProcessor for each plan
     */
    @Retryable(
        value = [OptimisticLockingFailureException::class],
        maxAttempts = MAX_RETRY_ATTEMPTS,
        backoff = Backoff(delay = 100, multiplier = 2.0)
    )
    private suspend fun processPlan(plan: Plan): Plan? {
        return try {
            // Check if plan already exists
            val existingPlan = planRepository.findByProviderId(plan.providerPlanId)

            if (existingPlan != null) {
                // Update existing plan
                updateExistingPlan(existingPlan, plan)
            } else {
                // Create new plan
                createNewPlan(plan)
            }

        } catch (e: OptimisticLockingFailureException) {
            logger.warn("Optimistic locking failure for plan ${plan.providerPlanId}, retrying...")
            throw e // Will trigger retry functionality
        } catch (e: Exception) {
            logger.error("Failed to process plan ${plan.providerPlanId}", e)
            // Publish failure event
            domainEventPublisher.publish(
                PlanFailedEvent(
                    providerEventId = plan.providerPlanId,
                    failureReason = e.message ?: "Unknown error"
                )
            )
            null
        }
    }

    private suspend fun createNewPlan(plan: Plan): Plan {
        val savedPlan = planRepository.upsertPlan(plan)

        // Publish domain event
        domainEventPublisher.publish(
            PlanSyncedEvent(
                planEntityId = savedPlan.id,
                title = savedPlan.title,
                providerPlanId = savedPlan.providerPlanId,
                organizerCompanyId = savedPlan.organizerCompanyId,
                planDate = savedPlan.date,
                priceRange = savedPlan.priceRange,
                sellPeriod = savedPlan.sellPeriod,
                soldOut = savedPlan.soldOut,
                version = savedPlan.version
            )
        )
        logger.debug("Created new plan: {} ({})", savedPlan.title, savedPlan.providerPlanId)
        return savedPlan
    }

    /**
     * Update existing plan with change detection and domain events
     */
    private suspend fun updateExistingPlan(existingPlan: Plan, plan: Plan): Plan {
        // Detect changes for domain events
        val hasChanges = detectAndLogChanges(existingPlan, plan)

        if (!hasChanges) {
            logger.debug("No significant changes for plan: ${plan.title}")
            return existingPlan
        }

        // Create updated plan
        val updatedPlan = existingPlan.updateFromProvider(
            newTitle = plan.title,
            date = plan.date,
            newPriceRange = plan.priceRange,
            newSellFrom = plan.sellPeriod?.from ?: plan.date.from,
            newSellTo = plan.sellPeriod?.to ?: plan.date.to,
            newSoldOut = plan.soldOut,
            newOrganizerCompanyId = plan.organizerCompanyId,
            newZones = plan.zones,
        )

        val savedPlan = planRepository.upsertPlan(updatedPlan)

        // Publish update event with detailed change information
        domainEventPublisher.publish(
            PlanUpdatedEvent(
                planEntityId = savedPlan.id,
                providerPlanId = savedPlan.providerPlanId,
                previousVersion = existingPlan.version,
                newVersion = savedPlan.version,
                previousTitle = existingPlan.title,
                newTitle = savedPlan.title,
                previousPriceRange = existingPlan.priceRange,
                newPriceRange = savedPlan.priceRange,
                previousSellPeriod = existingPlan.sellPeriod,
                newSellPeriod = savedPlan.sellPeriod,
                previousSoldOut = existingPlan.soldOut,
                newSoldOut = savedPlan.soldOut,
                previousOrganizerCompanyId = existingPlan.organizerCompanyId,
                newOrganizerCompanyId = savedPlan.organizerCompanyId
            )
        )
        logger.debug("Updated plan: ${plan.title} (${plan.providerPlanId})")
        return savedPlan
    }

    /**
     * Detect significant changes that warrant domain events
     */
    private fun detectSignificantChanges(
        existingPlan: Plan,
        newPlan: Plan
    ): Boolean {
        return existingPlan.title != newPlan.title ||
                existingPlan.priceRange.min.compareTo(newPlan.priceRange.min) != 0 ||
                existingPlan.priceRange.max.compareTo(newPlan.priceRange.max) != 0 ||
                existingPlan.soldOut != newPlan.soldOut ||
                existingPlan.organizerCompanyId != newPlan.organizerCompanyId ||
                existingPlan.date != newPlan.date ||
                existingPlan.sellPeriod != newPlan.sellPeriod
    }

    private fun detectAndLogChanges(
        existingPlan: Plan,
        newPlan: Plan
    ): Boolean {
        val changes = mutableListOf<String>()

        // Title changes
        if (existingPlan.title != newPlan.title) {
            changes.add("title: '${existingPlan.title}' → '${newPlan.title}'")
        }

        // Price changes (with precision handling)
        if (!existingPlan.priceRange.isEquivalentTo(newPlan.priceRange)) {
            changes.add("price: ${existingPlan.priceRange.min}-${existingPlan.priceRange.max} → ${newPlan.priceRange.min}-${newPlan.priceRange.max}")
        }

        // Date changes (with minute precision)
        if (!existingPlan.date.isEquivalentTo(newPlan.date)) {
            changes.add("dates: ${existingPlan.date.from} to ${existingPlan.date.to} → ${newPlan.date.from} to ${newPlan.date.to}")
        }

        // Sold out status
        if (existingPlan.soldOut != newPlan.soldOut) {
            changes.add("soldOut: ${existingPlan.soldOut} → ${newPlan.soldOut}")
        }

        // Organizer company
        if (existingPlan.organizerCompanyId != newPlan.organizerCompanyId) {
            changes.add("organizer: '${existingPlan.organizerCompanyId}' → '${newPlan.organizerCompanyId}'")
        }

        // Sell period changes
        if (!sellPeriodsEquivalent(existingPlan.sellPeriod, newPlan.sellPeriod)) {
            changes.add("sellPeriod: ${existingPlan.sellPeriod} → ${newPlan.sellPeriod}")
        }

        // Log changes if any detected
        if (changes.isNotEmpty()) {
            logger.info("📝 Plan changes detected for '${newPlan.title}' (${newPlan.providerPlanId}): ${changes.joinToString(", ")}")
            return true
        }

        logger.debug("No significant changes for plan '${newPlan.title}' (${newPlan.providerPlanId})")
        return false
    }

    private fun sellPeriodsEquivalent(period1: DateRange?, period2: DateRange?): Boolean {
        return when {
            period1 == null && period2 == null -> true
            period1 == null || period2 == null -> false
            else -> period1.isEquivalentTo(period2)
        }
    }
}