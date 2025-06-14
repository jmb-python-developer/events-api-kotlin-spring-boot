package com.jmb.events_api.sync.application.service

import com.jmb.events_api.shared.domain.event.DomainEventPublisher
import com.jmb.events_api.sync.domain.event.PlanSyncedEvent
import com.jmb.events_api.sync.domain.event.PlanUpdatedEvent
import com.jmb.events_api.sync.domain.event.PlanFailedEvent
import com.jmb.events_api.sync.domain.model.*
import com.jmb.events_api.sync.domain.repository.PlanRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import org.springframework.dao.OptimisticLockingFailureException
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.LocalDateTime

@DisplayName("SyncPlansService Application Service Tests")
class SyncPlansServiceTest {

    private val planRepository = mockk<PlanRepository>()
    private val domainEventPublisher = mockk<DomainEventPublisher>()
    private val service = SyncPlansService(planRepository, domainEventPublisher)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        every { domainEventPublisher.publish(any<PlanSyncedEvent>()) } just Runs
        every { domainEventPublisher.publish(any<PlanUpdatedEvent>()) } just Runs
        every { domainEventPublisher.publish(any<PlanFailedEvent>()) } just Runs
    }

    private fun createTestPlan(
        id: String = "test-plan-id",
        providerPlanId: String = "provider-123",
        title: String = "Test Concert",
        version: Long = 1
    ): Plan {
        return Plan.createOnline(
            id = PlanId.of(id),
            title = title,
            date = DateRange(
                from = LocalDateTime.of(2024, 6, 15, 20, 0),
                to = LocalDateTime.of(2024, 6, 15, 23, 0)
            ),
            priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("50.00")),
            providerPlanId = providerPlanId,
            zones = listOf(
                Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            ),
            version = version
        )
    }

    @Nested
    @DisplayName("New Plan Creation")
    inner class NewPlanCreation {

        @Test
        fun `should create new plan when not found in repository`() = runTest {
            val newPlan = createTestPlan()
            every { planRepository.findByProviderId("provider-123") } returns null
            every { planRepository.upsertPlan(newPlan) } returns newPlan

            val result = service.syncPlans(listOf(newPlan))

            assertEquals(1, result.size)
            assertEquals(newPlan, result.first())

            verify(exactly = 1) { planRepository.findByProviderId("provider-123") }
            verify(exactly = 1) { planRepository.upsertPlan(newPlan) }
            verify(exactly = 1) { domainEventPublisher.publish(any<PlanSyncedEvent>()) }
        }

        @Test
        fun `should publish PlanSyncedEvent with correct data when creating new plan`() = runTest {
            val newPlan = createTestPlan()
            val savedPlan = newPlan
            every { planRepository.findByProviderId("provider-123") } returns null
            every { planRepository.upsertPlan(newPlan) } returns savedPlan

            val eventSlot = slot<PlanSyncedEvent>()
            every { domainEventPublisher.publish(capture<PlanSyncedEvent>(eventSlot)) } just Runs

            service.syncPlans(listOf(newPlan))

            val publishedEvent = eventSlot.captured
            assertEquals(savedPlan.id, publishedEvent.planEntityId)
            assertEquals(savedPlan.title, publishedEvent.title)
            assertEquals(savedPlan.providerPlanId, publishedEvent.providerPlanId)
            assertEquals(savedPlan.priceRange, publishedEvent.priceRange)
            assertEquals(savedPlan.version, publishedEvent.version)
        }

        @Test
        fun `should handle multiple new plans in batch`() = runTest {
            val plan1 = createTestPlan(id = "plan-1", providerPlanId = "provider-1", title = "Concert 1")
            val plan2 = createTestPlan(id = "plan-2", providerPlanId = "provider-2", title = "Concert 2")
            val plans = listOf(plan1, plan2)

            every { planRepository.findByProviderId("provider-1") } returns null
            every { planRepository.findByProviderId("provider-2") } returns null
            every { planRepository.upsertPlan(plan1) } returns plan1
            every { planRepository.upsertPlan(plan2) } returns plan2

            val result = service.syncPlans(plans)

            assertEquals(2, result.size)
            verify(exactly = 2) { planRepository.findByProviderId(any()) }
            verify(exactly = 2) { planRepository.upsertPlan(any()) }
            verify(exactly = 2) { domainEventPublisher.publish(any<PlanSyncedEvent>()) }
        }
    }

    @Nested
    @DisplayName("Plan Updates")
    inner class PlanUpdates {

        @Test
        fun `should update existing plan when changes are detected`() = runTest {
            val existingPlan = createTestPlan(title = "Original Title", version = 1)
            val incomingPlan = createTestPlan(title = "Updated Title", version = 1)

            every { planRepository.findByProviderId("provider-123") } returns existingPlan

            val updatedPlan = existingPlan.updateFromProvider(
                newTitle = incomingPlan.title,
                date = incomingPlan.date,
                newPriceRange = incomingPlan.priceRange,
                newSellFrom = incomingPlan.date.from,
                newSellTo = incomingPlan.date.to,
                newSoldOut = incomingPlan.soldOut,
                newOrganizerCompanyId = incomingPlan.organizerCompanyId,
                newZones = incomingPlan.zones
            )

            every { planRepository.upsertPlan(any()) } returns updatedPlan

            val result = service.syncPlans(listOf(incomingPlan))

            assertEquals(1, result.size)
            assertEquals(updatedPlan, result.first())
            verify(exactly = 1) { planRepository.findByProviderId("provider-123") }
            verify(exactly = 1) { planRepository.upsertPlan(any()) }
        }

        @Test
        fun `should not update plan when no changes are detected`() = runTest {
            val existingPlan = createTestPlan()
            val identicalPlan = createTestPlan()

            every { planRepository.findByProviderId("provider-123") } returns existingPlan

            val result = service.syncPlans(listOf(identicalPlan))

            assertEquals(1, result.size)
            assertEquals(existingPlan, result.first())

            verify(exactly = 1) { planRepository.findByProviderId("provider-123") }
            verify(exactly = 0) { planRepository.upsertPlan(any()) }
        }

        @Test
        fun `should publish PlanUpdatedEvent with change details`() = runTest {
            val existingPlan = createTestPlan(
                title = "Original Concert",
                version = 1
            )
            val incomingPlan = createTestPlan(
                title = "Updated Concert",
                version = 1
            )

            every { planRepository.findByProviderId("provider-123") } returns existingPlan

            val updatedPlan = existingPlan.updateFromProvider(
                newTitle = "Updated Concert",
                date = incomingPlan.date,
                newPriceRange = incomingPlan.priceRange,
                newSellFrom = incomingPlan.date.from,
                newSellTo = incomingPlan.date.to,
                newSoldOut = incomingPlan.soldOut,
                newOrganizerCompanyId = incomingPlan.organizerCompanyId,
                newZones = incomingPlan.zones
            )

            every { planRepository.upsertPlan(any()) } returns updatedPlan

            val eventSlot = slot<PlanUpdatedEvent>()
            every { domainEventPublisher.publish(capture(eventSlot)) } just Runs

            service.syncPlans(listOf(incomingPlan))

            val publishedEvent = eventSlot.captured
            assertEquals(updatedPlan.id, publishedEvent.planEntityId)
            assertEquals("Original Concert", publishedEvent.previousTitle)
            assertEquals("Updated Concert", publishedEvent.newTitle)
            assertEquals(1, publishedEvent.previousVersion)
            assertEquals(2, publishedEvent.newVersion)
        }

        @Test
        fun `should detect multiple types of changes`() = runTest {
            val existingPlan = createTestPlan(
                title = "Original Concert",
                version = 1
            ).let { plan ->
                Plan.createOnline(
                    id = plan.id,
                    title = "Original Concert",
                    date = plan.date,
                    priceRange = PriceRange(BigDecimal("15.00"), BigDecimal("30.00")),
                    providerPlanId = plan.providerPlanId,
                    zones = plan.zones,
                    soldOut = false,
                    organizerCompanyId = "company-1",
                    version = 1
                )
            }

            val incomingPlan = createTestPlan(
                title = "Updated Concert",
                version = 1
            ).let { plan ->
                Plan.createOnline(
                    id = plan.id,
                    title = "Updated Concert",
                    date = plan.date,
                    priceRange = PriceRange(BigDecimal("25.00"), BigDecimal("60.00")),
                    providerPlanId = plan.providerPlanId,
                    zones = plan.zones,
                    soldOut = true,
                    organizerCompanyId = "company-2",
                    version = 1
                )
            }

            every { planRepository.findByProviderId("provider-123") } returns existingPlan
            every { planRepository.upsertPlan(any()) } returns incomingPlan.updateFromProvider(
                newTitle = incomingPlan.title,
                date = incomingPlan.date,
                newPriceRange = incomingPlan.priceRange,
                newSellFrom = incomingPlan.date.from,
                newSellTo = incomingPlan.date.to,
                newSoldOut = incomingPlan.soldOut,
                newOrganizerCompanyId = incomingPlan.organizerCompanyId,
                newZones = incomingPlan.zones
            )

            val result = service.syncPlans(listOf(incomingPlan))

            assertEquals(1, result.size)
            verify(exactly = 1) { planRepository.upsertPlan(any()) }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        fun `should handle OptimisticLockingFailureException with retry`() = runTest {
            val plan = createTestPlan()
            every { planRepository.findByProviderId("provider-123") } returns null
            every { planRepository.upsertPlan(plan) } throws OptimisticLockingFailureException("Optimistic lock failed")

            val result = service.syncPlans(listOf(plan))

            assertTrue(result.isEmpty())
            verify(exactly = 1) { planRepository.upsertPlan(plan) }
        }

        @Test
        fun `should publish failure event when plan processing fails`() = runTest {
            val plan = createTestPlan()
            val exception = RuntimeException("Database connection failed")

            every { planRepository.findByProviderId("provider-123") } throws exception

            val eventSlot = slot<PlanFailedEvent>()
            every { domainEventPublisher.publish(capture<PlanFailedEvent>(eventSlot)) } just Runs

            val result = service.syncPlans(listOf(plan))

            assertTrue(result.isEmpty())

            val publishedEvent = eventSlot.captured
            assertEquals("provider-123", publishedEvent.providerEventId)
            assertEquals("Database connection failed", publishedEvent.failureReason)
        }

        @Test
        fun `should continue processing other plans when one fails`() = runTest {
            val successfulPlan = createTestPlan(providerPlanId = "provider-success")
            val failingPlan = createTestPlan(providerPlanId = "provider-fail")
            val plans = listOf(successfulPlan, failingPlan)

            every { planRepository.findByProviderId("provider-success") } returns null
            every { planRepository.findByProviderId("provider-fail") } throws RuntimeException("DB Error")
            every { planRepository.upsertPlan(successfulPlan) } returns successfulPlan

            val result = service.syncPlans(plans)

            assertEquals(1, result.size)
            assertEquals(successfulPlan, result.first())
        }
    }

    @Nested
    @DisplayName("Business Logic Edge Cases")
    inner class BusinessLogicEdgeCases {

        @Test
        fun `should handle plans with identical content but different IDs`() = runTest {
            val existingPlan = createTestPlan(id = "existing-id")
            val incomingPlan = createTestPlan(id = "different-id")

            every { planRepository.findByProviderId("provider-123") } returns existingPlan

            val result = service.syncPlans(listOf(incomingPlan))

            assertEquals(1, result.size)
            assertEquals(existingPlan, result.first())
            verify(exactly = 0) { planRepository.upsertPlan(any()) }
        }

        @Test
        fun `should handle empty plan collection`() = runTest {
            val result = service.syncPlans(emptyList())

            assertTrue(result.isEmpty())
            verify(exactly = 0) { planRepository.findByProviderId(any()) }
            verify(exactly = 0) { planRepository.upsertPlan(any()) }
        }

        @Test
        fun `should handle plans with minimal differences requiring precision comparison`() = runTest {
            val existingPlan = createTestPlan().let { plan ->
                Plan.createOnline(
                    id = plan.id,
                    title = plan.title,
                    date = plan.date,
                    priceRange = PriceRange(BigDecimal("20.01"), BigDecimal("50.01")),
                    providerPlanId = plan.providerPlanId,
                    zones = plan.zones
                )
            }

            val incomingPlan = createTestPlan().let { plan ->
                Plan.createOnline(
                    id = plan.id,
                    title = plan.title,
                    date = plan.date,
                    priceRange = PriceRange(BigDecimal("20.009"), BigDecimal("50.009")),
                    providerPlanId = plan.providerPlanId,
                    zones = plan.zones
                )
            }

            every { planRepository.findByProviderId("provider-123") } returns existingPlan

            val result = service.syncPlans(listOf(incomingPlan))

            assertEquals(1, result.size)
            verify(exactly = 0) { planRepository.upsertPlan(any()) }
        }
    }
}