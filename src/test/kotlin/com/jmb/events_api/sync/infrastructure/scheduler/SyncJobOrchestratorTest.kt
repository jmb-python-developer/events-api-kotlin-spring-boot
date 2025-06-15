package com.jmb.events_api.sync.infrastructure.scheduler

import com.jmb.events_api.sync.application.dto.ProviderPlanDto
import com.jmb.events_api.sync.application.dto.ZoneDto
import com.jmb.events_api.sync.infrastructure.external.ProviderApiClient
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.mockk.*
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.concurrent.TimeoutException

@DisplayName("SyncJobOrchestrator Infrastructure Tests")
class SyncJobOrchestratorTest {

    private val providerApiClient = mockk<ProviderApiClient>()
    private val syncBatchProcessor = mockk<SyncBatchProcessor>()

    private val orchestrator = SyncJobOrchestrator(
        providerApiClient = providerApiClient,
        syncBatchProcessor = syncBatchProcessor
    )

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createTestProviderPlan(
        basePlanId: String = "plan-123",
        title: String = "Test Concert",
        soldOut: Boolean = false
    ): ProviderPlanDto {
        return ProviderPlanDto(
            basePlanId = basePlanId,
            title = title,
            sellMode = "online",
            organizerCompanyId = "company-1",
            planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
            planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
            sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
            sellTo = LocalDateTime.of(2024, 6, 15, 19, 0),
            soldOut = soldOut,
            zones = listOf(
                ZoneDto("zone-1", "General", BigDecimal("25.00"), 100, false),
                ZoneDto("zone-2", "VIP", BigDecimal("75.00"), 50, true)
            )
        )
    }

    private fun createBatchProcessingResult(
        totalPlans: Int = 10,
        successfulPlans: Int = 8,
        failedPlans: Int = 2,
        totalBatches: Int = 1,
        successfulBatches: Int = 1,
        failedBatches: Int = 0,
        partiallyFailedBatches: Int = 0,
        mappingFailures: Int = 1,
        serviceFailures: Int = 1
    ): BatchProcessingResult {
        return BatchProcessingResult(
            totalBatches = totalBatches,
            successfulBatches = successfulBatches,
            failedBatches = failedBatches,
            partiallyFailedBatches = partiallyFailedBatches,
            totalPlans = totalPlans,
            successfulPlans = successfulPlans,
            failedPlans = failedPlans,
            mappingFailures = mappingFailures,
            serviceFailures = serviceFailures
        )
    }

    private fun createBatchResult(
        batchNumber: Int = 0,
        plansProcessed: Int = 10,
        successCount: Int = 8,
        errorCount: Int = 2,
        mappingFailures: Int = 1,
        serviceFailures: Int = 1
    ): BatchResult {
        return BatchResult(
            batchNumber = batchNumber,
            plansProcessed = plansProcessed,
            successCount = successCount,
            errorCount = errorCount,
            mappingFailures = mappingFailures,
            serviceFailures = serviceFailures
        )
    }

    @Nested
    @DisplayName("Circuit Breaker Integration")
    inner class CircuitBreakerIntegration {

        @Test
        fun `should proceed with sync when circuit breaker is CLOSED`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(10, result.totalPlans)
            assertEquals(8, result.successfulPlans)
            assertEquals(2, result.failedPlans)

            coVerify(exactly = 1) { providerApiClient.fetchPlans() }
            coVerify(exactly = 1) { syncBatchProcessor.processBatch(testPlans, 20) }
        }

        @Test
        fun `should proceed with sync when circuit breaker is HALF_OPEN`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.HALF_OPEN
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            coVerify(exactly = 1) { providerApiClient.fetchPlans() }
            coVerify(exactly = 1) { syncBatchProcessor.processBatch(testPlans, 20) }
        }

        @Test
        fun `should proceed with sync when circuit breaker is DISABLED`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.DISABLED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            coVerify(exactly = 1) { providerApiClient.fetchPlans() }
            coVerify(exactly = 1) { syncBatchProcessor.processBatch(testPlans, 20) }
        }

        @Test
        fun `should skip sync when circuit breaker is OPEN`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.OPEN

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.first().contains("Circuit Breaker is OPEN"))

            coVerify(exactly = 0) { providerApiClient.fetchPlans() }
            coVerify(exactly = 0) { syncBatchProcessor.processBatch(any(), any()) }
        }

        @Test
        fun `should skip sync when circuit breaker is FORCED_OPEN`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.FORCED_OPEN

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.first().contains("Circuit Breaker is FORCED_OPEN"))

            coVerify(exactly = 0) { providerApiClient.fetchPlans() }
            coVerify(exactly = 0) { syncBatchProcessor.processBatch(any(), any()) }
        }

        @Test
        fun `should proceed with sync when circuit breaker is METRICS_ONLY`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.METRICS_ONLY
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            coVerify(exactly = 1) { providerApiClient.fetchPlans() }
            coVerify(exactly = 1) { syncBatchProcessor.processBatch(testPlans, 20) }
        }
    }

    @Nested
    @DisplayName("Successful Sync Scenarios")
    inner class SuccessfulSyncScenarios {

        @Test
        fun `should handle successful sync with multiple plans`() = runTest {
            val testPlans = listOf(
                createTestProviderPlan("plan-1", "Concert 1"),
                createTestProviderPlan("plan-2", "Concert 2"),
                createTestProviderPlan("plan-3", "Concert 3")
            )
            val batchResult = createBatchProcessingResult(
                totalPlans = 3,
                successfulPlans = 3,
                failedPlans = 0,
                totalBatches = 1,
                successfulBatches = 1,
                failedBatches = 0,
                partiallyFailedBatches = 0,
                mappingFailures = 0,
                serviceFailures = 0
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(3, result.totalPlans)
            assertEquals(3, result.successfulPlans)
            assertEquals(0, result.failedPlans)
            assertTrue(result.errors.isEmpty())
        }

        @Test
        fun `should handle successful sync with empty plans list`() = runTest {
            val emptyPlans = emptyList<ProviderPlanDto>()
            val emptyBatchResult = createBatchProcessingResult(
                totalPlans = 0,
                successfulPlans = 0,
                failedPlans = 0,
                totalBatches = 0,
                successfulBatches = 0,
                failedBatches = 0,
                partiallyFailedBatches = 0,
                mappingFailures = 0,
                serviceFailures = 0
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns emptyPlans
            coEvery { syncBatchProcessor.processBatch(emptyPlans, 20) } returns emptyBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(0, result.totalPlans)
            assertEquals(0, result.successfulPlans)
            assertEquals(0, result.failedPlans)
        }

        @Test
        fun `should handle partial success scenario`() = runTest {
            val testPlans = listOf(
                createTestProviderPlan("plan-1", "Working Concert"),
                createTestProviderPlan("plan-2", "Failing Concert")
            )
            val partialBatchResult = createBatchProcessingResult(
                totalPlans = 2,
                successfulPlans = 1,
                failedPlans = 1,
                totalBatches = 1,
                successfulBatches = 0,
                failedBatches = 0,
                partiallyFailedBatches = 1, // This batch had some successes and some failures
                mappingFailures = 0,
                serviceFailures = 1
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns partialBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(2, result.totalPlans)
            assertEquals(1, result.successfulPlans)
            assertEquals(1, result.failedPlans)
        }

        @Test
        fun `should use correct batch size for processing`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            orchestrator.orchestrateFullSync()

            coVerify(exactly = 1) { syncBatchProcessor.processBatch(testPlans, 20) }
        }

        @Test
        fun `should handle detailed batch metrics correctly`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val detailedBatchResult = createBatchProcessingResult(
                totalPlans = 20,
                successfulPlans = 15,
                failedPlans = 5,
                totalBatches = 4,
                successfulBatches = 2,
                failedBatches = 1,
                partiallyFailedBatches = 1,
                mappingFailures = 3,
                serviceFailures = 2
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns detailedBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(20, result.totalPlans)
            assertEquals(15, result.successfulPlans)
            assertEquals(5, result.failedPlans)
            // Verify that detailed metrics are preserved
            assertTrue(result.errors.isEmpty())
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        fun `should handle provider API client exceptions`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } throws RuntimeException("Provider API failed")

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertEquals("Provider API failed", result.errors.first())

            coVerify(exactly = 1) { providerApiClient.fetchPlans() }
            coVerify(exactly = 0) { syncBatchProcessor.processBatch(any(), any()) }
        }

        @Test
        fun `should handle batch processor exceptions`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery {
                syncBatchProcessor.processBatch(
                    testPlans,
                    20
                )
            } throws RuntimeException("Batch processing failed")

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertEquals("Batch processing failed", result.errors.first())
        }

        @Test
        fun `should handle null exception messages`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } throws RuntimeException(null as String?)

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertEquals("Unknown error", result.errors.first())
        }

        @Test
        fun `should handle timeout exceptions`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } throws TimeoutException("Request timeout")

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertTrue(result.errors.first().contains("Request timeout"))
        }

        @Test
        fun `should handle interrupted exceptions`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } throws InterruptedException("Thread interrupted")

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertTrue(result.errors.first().contains("Thread interrupted"))
        }

        @Test
        fun `should handle circuit breaker state check exceptions`() = runTest {
            every { providerApiClient.getCircuitBreakerState() } throws RuntimeException("Circuit breaker check failed")

            val result = orchestrator.orchestrateFullSync()

            assertFalse(result.success)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.first().contains("Circuit breaker check failed"))
        }
    }

    @Nested
    @DisplayName("Circuit Breaker State Checking")
    inner class CircuitBreakerStateChecking {

        @Test
        fun `circuitBreakerCheck should return correct state for CLOSED`() {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED

            val state = orchestrator.circuitBreakerCheck()

            assertEquals(CircuitBreaker.State.CLOSED, state)
            verify(exactly = 1) { providerApiClient.getCircuitBreakerState() }
        }

        @Test
        fun `circuitBreakerCheck should return correct state for OPEN`() {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.OPEN

            val state = orchestrator.circuitBreakerCheck()

            assertEquals(CircuitBreaker.State.OPEN, state)
            verify(exactly = 1) { providerApiClient.getCircuitBreakerState() }
        }

        @Test
        fun `circuitBreakerCheck should return correct state for HALF_OPEN`() {
            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.HALF_OPEN

            val state = orchestrator.circuitBreakerCheck()

            assertEquals(CircuitBreaker.State.HALF_OPEN, state)
            verify(exactly = 1) { providerApiClient.getCircuitBreakerState() }
        }

        @Test
        fun `circuitBreakerCheck should handle all possible states`() {
            val allStates = CircuitBreaker.State.entries.toTypedArray()

            allStates.forEach { expectedState ->
                clearMocks(providerApiClient)
                every { providerApiClient.getCircuitBreakerState() } returns expectedState

                val actualState = orchestrator.circuitBreakerCheck()

                assertEquals(expectedState, actualState)
            }
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    inner class IntegrationScenarios {

        @Test
        fun `should handle complete workflow with realistic data`() = runTest {
            val realisticPlans = listOf(
                ProviderPlanDto(
                    basePlanId = "concert-001",
                    title = "Rock Concert 2024",
                    sellMode = "online",
                    organizerCompanyId = "rock-events-inc",
                    planStartDate = LocalDateTime.of(2024, 8, 15, 20, 0),
                    planEndDate = LocalDateTime.of(2024, 8, 15, 23, 30),
                    sellFrom = LocalDateTime.of(2024, 6, 1, 9, 0),
                    sellTo = LocalDateTime.of(2024, 8, 15, 18, 0),
                    soldOut = false,
                    zones = listOf(
                        ZoneDto("ga", "General Admission", BigDecimal("45.00"), 5000, false),
                        ZoneDto("vip", "VIP Section", BigDecimal("150.00"), 200, true),
                        ZoneDto("backstage", "Backstage Pass", BigDecimal("300.00"), 50, true)
                    )
                ),
                ProviderPlanDto(
                    basePlanId = "theater-002",
                    title = "Shakespeare Festival",
                    sellMode = "online",
                    organizerCompanyId = "cultural-events",
                    planStartDate = LocalDateTime.of(2024, 7, 20, 19, 30),
                    planEndDate = LocalDateTime.of(2024, 7, 20, 22, 0),
                    sellFrom = LocalDateTime.of(2024, 5, 15, 10, 0),
                    sellTo = LocalDateTime.of(2024, 7, 20, 17, 0),
                    soldOut = true,
                    zones = listOf(
                        ZoneDto("orchestra", "Orchestra", BigDecimal("75.00"), 300, true),
                        ZoneDto("balcony", "Balcony", BigDecimal("50.00"), 150, true)
                    )
                )
            )

            val realisticBatchResult = BatchProcessingResult(
                totalBatches = 1,
                successfulBatches = 1,
                failedBatches = 0,
                partiallyFailedBatches = 0,
                totalPlans = 2,
                successfulPlans = 2,
                failedPlans = 0,
                mappingFailures = 0,
                serviceFailures = 0
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns realisticPlans
            coEvery { syncBatchProcessor.processBatch(realisticPlans, 20) } returns realisticBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(2, result.totalPlans)
            assertEquals(2, result.successfulPlans)
            assertEquals(0, result.failedPlans)
            assertTrue(result.errors.isEmpty())
        }

        @Test
        fun `should handle large number of plans efficiently`() = runTest {
            val largePlansList = (1..1000).map { index ->
                createTestProviderPlan("plan-$index", "Event $index")
            }
            val largeBatchResult = createBatchProcessingResult(
                totalPlans = 1000,
                successfulPlans = 995,
                failedPlans = 5,
                totalBatches = 50, // 1000 plans / 20 per batch
                successfulBatches = 45,
                failedBatches = 2,
                partiallyFailedBatches = 3,
                mappingFailures = 3,
                serviceFailures = 2
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns largePlansList
            coEvery { syncBatchProcessor.processBatch(largePlansList, 20) } returns largeBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(1000, result.totalPlans)
            assertEquals(995, result.successfulPlans)
            assertEquals(5, result.failedPlans)
        }

        @Test
        fun `should handle mixed success and failure batches`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val mixedBatchResult = BatchProcessingResult(
                totalBatches = 5,
                successfulBatches = 3,
                failedBatches = 2,
                partiallyFailedBatches = 0,
                totalPlans = 100,
                successfulPlans = 75,
                failedPlans = 25,
                mappingFailures = 15,
                serviceFailures = 10
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns mixedBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(100, result.totalPlans)
            assertEquals(75, result.successfulPlans)
            assertEquals(25, result.failedPlans)
        }

        @Test
        fun `should handle all failure types correctly`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val failureBatchResult = createBatchProcessingResult(
                totalPlans = 50,
                successfulPlans = 30,
                failedPlans = 20,
                totalBatches = 3,
                successfulBatches = 1,
                failedBatches = 1,
                partiallyFailedBatches = 1,
                mappingFailures = 12, // Failures in DTO mapping
                serviceFailures = 8   // Failures in service layer
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns failureBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success) // Success because some plans were processed
            assertEquals(50, result.totalPlans)
            assertEquals(30, result.successfulPlans)
            assertEquals(20, result.failedPlans)
            // The batch result should contain detailed failure breakdown
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should handle plans with null or empty zones`() = runTest {
            val planWithEmptyZones = ProviderPlanDto(
                basePlanId = "plan-no-zones",
                title = "Event Without Zones",
                sellMode = "online",
                organizerCompanyId = "test-company",
                planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
                sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 6, 15, 19, 0),
                soldOut = false,
                zones = emptyList() // Empty zones
            )
            val batchResult = createBatchProcessingResult(
                totalPlans = 1,
                successfulPlans = 0,
                failedPlans = 1,
                mappingFailures = 1, // Failed during mapping due to empty zones
                serviceFailures = 0
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns listOf(planWithEmptyZones)
            coEvery { syncBatchProcessor.processBatch(listOf(planWithEmptyZones), 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(1, result.totalPlans)
            assertEquals(0, result.successfulPlans)
            assertEquals(1, result.failedPlans)
        }

        @Test
        fun `should handle plans with extreme price values`() = runTest {
            val extremePricePlan = ProviderPlanDto(
                basePlanId = "plan-extreme-prices",
                title = "Expensive Event",
                sellMode = "online",
                organizerCompanyId = "luxury-events",
                planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
                sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 6, 15, 19, 0),
                soldOut = false,
                zones = listOf(
                    ZoneDto("cheap", "Budget", BigDecimal("0.01"), 1000, false),
                    ZoneDto("expensive", "Premium", BigDecimal("99999.99"), 1, true)
                )
            )
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns listOf(extremePricePlan)
            coEvery { syncBatchProcessor.processBatch(listOf(extremePricePlan), 20) } returns batchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertNotNull(result.totalPlans)
        }

        @Test
        fun `should handle concurrent orchestration requests`() = runTest {
            val testPlans = listOf(createTestProviderPlan())
            val batchResult = createBatchProcessingResult()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns batchResult

            // Execute multiple concurrent sync operations
            val results = listOf(
                async { orchestrator.orchestrateFullSync() },
                async { orchestrator.orchestrateFullSync() },
                async { orchestrator.orchestrateFullSync() }
            ).map { it.await() }

            // All should succeed independently
            results.forEach { result ->
                assertTrue(result.success)
                assertEquals(10, result.totalPlans)
            }

            // Should have made multiple calls to dependencies
            coVerify(exactly = 3) { providerApiClient.fetchPlans() }
            coVerify(exactly = 3) { syncBatchProcessor.processBatch(testPlans, 20) }
        }
    }

    @Nested
    @DisplayName("Enhanced Batch Processing Tests")
    inner class EnhancedBatchProcessingTests {

        @Test
        fun `should handle batch results with detailed failure categorization`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            // Create a realistic batch result with different types of failures
            val detailedBatchResult = BatchProcessingResult(
                totalBatches = 3,
                successfulBatches = 1,        // 1 batch completely successful
                failedBatches = 1,           // 1 batch completely failed
                partiallyFailedBatches = 1,  // 1 batch with mixed results
                totalPlans = 30,
                successfulPlans = 20,
                failedPlans = 10,
                mappingFailures = 6,         // 6 failures during DTO mapping
                serviceFailures = 4          // 4 failures during service processing
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns detailedBatchResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(30, result.totalPlans)
            assertEquals(20, result.successfulPlans)
            assertEquals(10, result.failedPlans)

            // Verify that the orchestrator properly handles the enhanced metrics
            assertTrue(result.errors.isEmpty()) // Should still be success since some plans processed
        }

        @Test
        fun `should use BatchProcessingResult factory methods correctly`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            // Test empty result
            val emptyResult = BatchProcessingResult.empty()

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns emptyList()
            coEvery { syncBatchProcessor.processBatch(emptyList(), 20) } returns emptyResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(0, result.totalPlans)
            assertEquals(0, result.successfulPlans)
            assertEquals(0, result.failedPlans)
        }

        @Test
        fun `should handle BatchResult with enhanced failure tracking`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            // Create batch results that would be used by BatchProcessingResult.fromBatchResults
            val batchResults = listOf(
                createBatchResult(
                    batchNumber = 0,
                    plansProcessed = 10,
                    successCount = 8,
                    errorCount = 2,
                    mappingFailures = 1,
                    serviceFailures = 1
                ),
                createBatchResult(
                    batchNumber = 1,
                    plansProcessed = 5,
                    successCount = 5,
                    errorCount = 0,
                    mappingFailures = 0,
                    serviceFailures = 0
                )
            )

            val aggregatedResult = BatchProcessingResult.fromBatchResults(batchResults, 15)

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns aggregatedResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(15, result.totalPlans)
            assertEquals(13, result.successfulPlans) // 8 + 5
            assertEquals(2, result.failedPlans)      // 2 + 0
        }

        @Test
        fun `should verify BatchResult computed properties work correctly`() {
            // Test isSuccessful property
            val successfulBatch = createBatchResult(errorCount = 0)
            assertTrue(successfulBatch.isSuccessful)

            val failedBatch = createBatchResult(errorCount = 5)
            assertFalse(failedBatch.isSuccessful)

            // Test hasPartialFailures property
            val partialBatch = createBatchResult(successCount = 5, errorCount = 3)
            assertTrue(partialBatch.hasPartialFailures)

            val completeBatch = createBatchResult(successCount = 8, errorCount = 0)
            assertFalse(completeBatch.hasPartialFailures)

            val completeFailBatch = createBatchResult(successCount = 0, errorCount = 8)
            assertFalse(completeFailBatch.hasPartialFailures)
        }

        @Test
        fun `should handle various batch failure scenarios`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            // Scenario: All mapping failures
            val mappingFailureResult = createBatchProcessingResult(
                totalPlans = 10,
                successfulPlans = 0,
                failedPlans = 10,
                mappingFailures = 10,
                serviceFailures = 0
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns mappingFailureResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success) // Still success since operation completed
            assertEquals(10, result.totalPlans)
            assertEquals(0, result.successfulPlans)
            assertEquals(10, result.failedPlans)
        }

        @Test
        fun `should handle service layer failures separately from mapping failures`() = runTest {
            val testPlans = listOf(createTestProviderPlan())

            // Scenario: Mixed mapping and service failures
            val mixedFailureResult = createBatchProcessingResult(
                totalPlans = 20,
                successfulPlans = 10,
                failedPlans = 10,
                mappingFailures = 4,  // Failed to map DTO to domain
                serviceFailures = 6   // Failed in business logic/persistence
            )

            every { providerApiClient.getCircuitBreakerState() } returns CircuitBreaker.State.CLOSED
            coEvery { providerApiClient.fetchPlans() } returns testPlans
            coEvery { syncBatchProcessor.processBatch(testPlans, 20) } returns mixedFailureResult

            val result = orchestrator.orchestrateFullSync()

            assertTrue(result.success)
            assertEquals(20, result.totalPlans)
            assertEquals(10, result.successfulPlans)
            assertEquals(10, result.failedPlans)
            // Orchestrator doesn't need to know about failure types,
            // that's handled at the batch processor level
        }
    }
}