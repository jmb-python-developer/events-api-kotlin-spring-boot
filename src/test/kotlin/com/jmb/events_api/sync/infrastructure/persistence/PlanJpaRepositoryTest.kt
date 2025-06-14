package com.jmb.events_api.sync.infrastructure.persistence

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import kotlin.collections.all
import kotlin.collections.any
import kotlin.collections.forEach
import kotlin.collections.intersect
import kotlin.collections.isNotEmpty
import kotlin.collections.map
import kotlin.collections.toSet
import kotlin.ranges.until
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.text.startsWith

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PlanJpaRepository Integration Tests")
class PlanJpaRepositoryTest {

    @Autowired
    private lateinit var planRepository: PlanJpaRepository

    @Autowired
    private lateinit var entityManager: TestEntityManager

    private lateinit var testPlans: List<PlanJpaEntity>

    @BeforeEach
    fun setUp() {
        // Clear any existing data
        planRepository.deleteAll()
        entityManager.flush()
        entityManager.clear()

        testPlans = createTestPlans()
        testPlans.forEach { plan ->
            entityManager.persistAndFlush(plan)
        }
        entityManager.clear()
    }

    private fun createTestPlans(): List<PlanJpaEntity> {
        val now = Instant.now()

        return listOf(
            // Plan 1: Current concert - available, in date range
            PlanJpaEntity(
                id = "plan-current-concert",
                providerPlanId = "provider-001",
                title = "Rock Concert 2024",
                planStartDate = LocalDateTime.of(2024, 8, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 8, 15, 23, 0),
                priceRangeMin = BigDecimal("25.00"),
                priceRangeMax = BigDecimal("75.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "company-1",
                sellFrom = LocalDateTime.of(2024, 6, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 8, 15, 19, 0),
                soldOut = false,
                lastUpdated = now,
                version = 1
            ),

            // Plan 2: Sold out concert - same date range but unavailable
            PlanJpaEntity(
                id = "plan-sold-out",
                providerPlanId = "provider-002",
                title = "Popular Band - SOLD OUT",
                planStartDate = LocalDateTime.of(2024, 8, 16, 19, 30),
                planEndDate = LocalDateTime.of(2024, 8, 16, 22, 30),
                priceRangeMin = BigDecimal("40.00"),
                priceRangeMax = BigDecimal("120.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "company-2",
                sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 8, 16, 18, 0),
                soldOut = true, // SOLD OUT
                lastUpdated = now,
                version = 1
            ),

            // Plan 3: Future event - outside immediate date range
            PlanJpaEntity(
                id = "plan-future-event",
                providerPlanId = "provider-003",
                title = "New Year Concert 2025",
                planStartDate = LocalDateTime.of(2024, 12, 31, 21, 0),
                planEndDate = LocalDateTime.of(2025, 1, 1, 1, 0),
                priceRangeMin = BigDecimal("50.00"),
                priceRangeMax = BigDecimal("200.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "company-1",
                sellFrom = LocalDateTime.of(2024, 10, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 12, 31, 20, 0),
                soldOut = false,
                lastUpdated = now,
                version = 1
            ),

            // Plan 4: Budget concert - low price range
            PlanJpaEntity(
                id = "plan-budget",
                providerPlanId = "provider-004",
                title = "Community Festival",
                planStartDate = LocalDateTime.of(2024, 7, 20, 14, 0),
                planEndDate = LocalDateTime.of(2024, 7, 20, 18, 0),
                priceRangeMin = BigDecimal("5.00"),
                priceRangeMax = BigDecimal("15.00"),
                sellMode = "ONLINE",
                organizerCompanyId = null, // Test nullable company
                sellFrom = LocalDateTime.of(2024, 6, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 7, 20, 13, 0),
                soldOut = false,
                lastUpdated = now,
                version = 1
            ),

            // Plan 5: Premium event - high price range
            PlanJpaEntity(
                id = "plan-premium",
                providerPlanId = "provider-005",
                title = "VIP Gala Dinner",
                planStartDate = LocalDateTime.of(2024, 9, 5, 19, 0),
                planEndDate = LocalDateTime.of(2024, 9, 5, 23, 30),
                priceRangeMin = BigDecimal("150.00"),
                priceRangeMax = BigDecimal("500.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "premium-events",
                sellFrom = LocalDateTime.of(2024, 7, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 9, 5, 17, 0),
                soldOut = false,
                lastUpdated = now,
                version = 2
            ),

            // Plan 6: Edge case - same start/end time (short event)
            PlanJpaEntity(
                id = "plan-short",
                providerPlanId = "provider-006",
                title = "Quick Presentation",
                planStartDate = LocalDateTime.of(2024, 8, 10, 15, 0),
                planEndDate = LocalDateTime.of(2024, 8, 10, 15, 30),
                priceRangeMin = BigDecimal("0.00"), // Free event
                priceRangeMax = BigDecimal("0.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "education-corp",
                sellFrom = null, // Test nullable sell dates
                sellTo = null,
                soldOut = false,
                lastUpdated = now,
                version = 1
            )
        )
    }

    @Nested
    @DisplayName("Basic Repository Operations")
    inner class BasicOperations {

        @Test
        fun `should find plan by provider ID`() {
            val found = planRepository.findByProviderPlanId("provider-001")

            assertNotNull(found)
            assertEquals("Rock Concert 2024", found.title)
            assertEquals("plan-current-concert", found.id)
        }

        @Test
        fun `should return null for non-existent provider ID`() {
            val found = planRepository.findByProviderPlanId("non-existent")
            assertNull(found)
        }

        @Test
        fun `should save and retrieve plan with all fields`() {
            val newPlan = PlanJpaEntity(
                id = "test-save-plan",
                providerPlanId = "provider-new",
                title = "Test Save Concert",
                planStartDate = LocalDateTime.of(2024, 10, 1, 20, 0),
                planEndDate = LocalDateTime.of(2024, 10, 1, 23, 0),
                priceRangeMin = BigDecimal("30.00"),
                priceRangeMax = BigDecimal("80.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "test-company",
                sellFrom = LocalDateTime.of(2024, 8, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 10, 1, 19, 0),
                soldOut = false,
                lastUpdated = Instant.now(),
                version = 1
            )

            val saved = planRepository.save(newPlan)
            entityManager.flush()
            entityManager.clear()

            val retrieved = planRepository.findById(saved.id).orElse(null)
            assertNotNull(retrieved)
            assertEquals("Test Save Concert", retrieved.title)
            assertEquals(BigDecimal("30.00"), retrieved.priceRangeMin)
            assertEquals("test-company", retrieved.organizerCompanyId)
        }
    }

    @Nested
    @DisplayName("Date Range Queries")
    inner class DateRangeQueries {

        @Test
        fun `should find plans within exact date range`() {
            val startDate = LocalDateTime.of(2024, 8, 15, 0, 0)
            val endDate = LocalDateTime.of(2024, 8, 17, 23, 59)
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findPlansByDateRange(startDate, endDate, pageable)

            assertEquals(2, results.size) // Rock Concert + Sold Out Band
            assertTrue(results.any { it.title == "Rock Concert 2024" })
            assertTrue(results.any { it.title == "Popular Band - SOLD OUT" })
        }

        @Test
        fun `should return empty list for date range with no plans`() {
            val startDate = LocalDateTime.of(2025, 6, 1, 0, 0)
            val endDate = LocalDateTime.of(2025, 6, 30, 23, 59)
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findPlansByDateRange(startDate, endDate, pageable)

            assertTrue(results.isEmpty())
        }

        @Test
        fun `should respect pagination in date range queries`() {
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2025, 12, 31, 23, 59)

            // Get first page with size 2
            val firstPage = planRepository.findPlansByDateRange(
                startDate, endDate, PageRequest.of(0, 2)
            )

            // Get second page with size 2
            val secondPage = planRepository.findPlansByDateRange(
                startDate, endDate, PageRequest.of(1, 2)
            )

            assertEquals(2, firstPage.size)
            assertEquals(2, secondPage.size)

            // Ensure no overlap between pages
            val firstPageIds = firstPage.map { it.id }.toSet()
            val secondPageIds = secondPage.map { it.id }.toSet()
            assertTrue(firstPageIds.intersect(secondPageIds).isEmpty())
        }

        @Test
        fun `should order plans by start date ascending`() {
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2025, 12, 31, 23, 59)
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findPlansByDateRange(startDate, endDate, pageable)

            // Verify ascending order by start date
            for (i in 0 until results.size - 1) {
                assertTrue(
                    results[i].planStartDate.isBefore(results[i + 1].planStartDate) ||
                            results[i].planStartDate.isEqual(results[i + 1].planStartDate),
                    "Plans should be ordered by start date ascending"
                )
            }
        }

        @Test
        fun `should handle boundary date conditions`() {
            // Test exact boundary matches
            val exactStart = LocalDateTime.of(2024, 8, 15, 20, 0) // Exact start of Rock Concert
            val exactEnd = LocalDateTime.of(2024, 8, 15, 23, 0)   // Exact end of Rock Concert
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findPlansByDateRange(exactStart, exactEnd, pageable)

            assertEquals(1, results.size)
            assertEquals("Rock Concert 2024", results[0].title)
        }
    }

    @Nested
    @DisplayName("Available Plans Queries - Business Logic")
    inner class AvailablePlansQueries {

        @Test
        fun `should find only non-sold-out plans in date range`() {
            val startDate = LocalDateTime.of(2024, 8, 15, 0, 0)
            val endDate = LocalDateTime.of(2024, 8, 17, 23, 59)
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findAvailablePlansByDateRange(startDate, endDate, pageable)

            assertEquals(1, results.size) // Only Rock Concert, not the sold out one
            assertEquals("Rock Concert 2024", results[0].title)
            assertEquals(false, results[0].soldOut)
        }

        @Test
        fun `should exclude sold out plans from available query`() {
            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2025, 12, 31, 23, 59)
            val pageable = PageRequest.of(0, 20)

            val allPlans = planRepository.findPlansByDateRange(startDate, endDate, pageable)
            val availablePlans = planRepository.findAvailablePlansByDateRange(startDate, endDate, pageable)

            assertTrue(allPlans.size > availablePlans.size)
            assertTrue(availablePlans.all { !it.soldOut })
            assertTrue(allPlans.any { it.soldOut }) // Verify test data has sold out plans
        }

        @Test
        fun `should return empty list when all plans in range are sold out`() {
            // First, mark all plans as sold out
            val allPlans = planRepository.findAll()
            allPlans.forEach { plan ->
                val soldOutPlan = PlanJpaEntity(
                    id = plan.id,
                    providerPlanId = plan.providerPlanId,
                    title = plan.title,
                    planStartDate = plan.planStartDate,
                    planEndDate = plan.planEndDate,
                    priceRangeMin = plan.priceRangeMin,
                    priceRangeMax = plan.priceRangeMax,
                    sellMode = plan.sellMode,
                    organizerCompanyId = plan.organizerCompanyId,
                    sellFrom = plan.sellFrom,
                    sellTo = plan.sellTo,
                    soldOut = true, // Mark as sold out
                    lastUpdated = plan.lastUpdated,
                    version = plan.version
                )
                planRepository.save(soldOutPlan)
            }
            entityManager.flush()

            val startDate = LocalDateTime.of(2024, 8, 15, 0, 0)
            val endDate = LocalDateTime.of(2024, 8, 17, 23, 59)
            val pageable = PageRequest.of(0, 10)

            val results = planRepository.findAvailablePlansByDateRange(startDate, endDate, pageable)

            assertTrue(results.isEmpty())
        }
    }

    @Nested
    @DisplayName("Edge Cases and Data Integrity")
    inner class EdgeCasesAndDataIntegrity {

        @Test
        fun `should handle plans with null organizer company ID`() {
            val planWithNullCompany = planRepository.findByProviderPlanId("provider-004")

            assertNotNull(planWithNullCompany)
            assertEquals("Community Festival", planWithNullCompany.title)
            assertNull(planWithNullCompany.organizerCompanyId)
        }

        @Test
        fun `should handle plans with null sell dates`() {
            val planWithNullSellDates = planRepository.findByProviderPlanId("provider-006")

            assertNotNull(planWithNullSellDates)
            assertEquals("Quick Presentation", planWithNullSellDates.title)
            assertNull(planWithNullSellDates.sellFrom)
            assertNull(planWithNullSellDates.sellTo)
        }

        @Test
        fun `should preserve decimal precision for prices`() {
            val plan = planRepository.findByProviderPlanId("provider-001")

            assertNotNull(plan)
            assertEquals(BigDecimal("25.00"), plan.priceRangeMin)
            assertEquals(BigDecimal("75.00"), plan.priceRangeMax)

            // Verify scale is preserved
            assertEquals(2, plan.priceRangeMin.scale())
            assertEquals(2, plan.priceRangeMax.scale())
        }

        @Test
        fun `should handle very short duration events`() {
            val shortEvent = planRepository.findByProviderPlanId("provider-006")

            assertNotNull(shortEvent)
            assertEquals("Quick Presentation", shortEvent.title)

            // Verify 30-minute duration
            val duration = Duration.between(
                shortEvent.planStartDate,
                shortEvent.planEndDate
            )
            assertEquals(30, duration.toMinutes())
        }

        @Test
        fun `should handle concurrent access with optimistic locking`() {
            val plan = planRepository.findByProviderPlanId("provider-001")
            assertNotNull(plan)

            // Simulate optimistic locking by checking version
            val originalVersion = plan.version

            // Update plan
            val updatedPlan = PlanJpaEntity(
                id = plan.id,
                providerPlanId = plan.providerPlanId,
                title = "Updated " + plan.title,
                planStartDate = plan.planStartDate,
                planEndDate = plan.planEndDate,
                priceRangeMin = plan.priceRangeMin,
                priceRangeMax = plan.priceRangeMax,
                sellMode = plan.sellMode,
                organizerCompanyId = plan.organizerCompanyId,
                sellFrom = plan.sellFrom,
                sellTo = plan.sellTo,
                soldOut = plan.soldOut,
                lastUpdated = Instant.now(),
            )

            planRepository.save(updatedPlan)
            entityManager.flush()

            val reloaded = planRepository.findByProviderPlanId("provider-001")
            assertNotNull(reloaded)
            assertEquals(originalVersion + 1, reloaded.version)
            assertTrue(reloaded.title.startsWith("Updated"))
        }
    }

    @Nested
    @DisplayName("Performance and Index Usage")
    inner class PerformanceTests {

        @Test
        fun `date range queries should be fast with proper indexing`() {
            val startTime = System.currentTimeMillis()

            val startDate = LocalDateTime.of(2024, 1, 1, 0, 0)
            val endDate = LocalDateTime.of(2025, 12, 31, 23, 59)
            val pageable = PageRequest.of(0, 100)

            val results = planRepository.findPlansByDateRange(startDate, endDate, pageable)

            val executionTime = System.currentTimeMillis() - startTime

            // Should complete quickly (under 100ms for small dataset)
            assertTrue(executionTime < 100, "Query took ${executionTime}ms - may indicate missing index")
            assertTrue(results.isNotEmpty())
        }

        @Test
        fun `provider ID lookups should be fast with unique index`() {
            val startTime = System.currentTimeMillis()

            val result = planRepository.findByProviderPlanId("provider-001")

            val executionTime = System.currentTimeMillis() - startTime

            // Unique index lookup should be very fast
            assertTrue(executionTime < 50, "Provider ID lookup took ${executionTime}ms - may indicate missing index")
            assertNotNull(result)
        }
    }
}