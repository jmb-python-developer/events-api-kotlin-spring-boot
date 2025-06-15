package com.jmb.events_api.sync.infrastructure.scheduler

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.atomic.AtomicBoolean
import javax.sql.DataSource

@DisplayName("PlanSyncScheduler Infrastructure Tests")
class PlanSyncSchedulerTest {

    private val syncJobOrchestrator = mockk<SyncJobOrchestrator>()
    private val dataSource = mockk<DataSource>()
    private val connection = mockk<Connection>()

    private lateinit var scheduler: PlanSyncScheduler

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        scheduler = PlanSyncScheduler(
            syncJobOrchestrator = syncJobOrchestrator,
            dataSource = dataSource,
            syncingEnabled = true
        )
        every { dataSource.connection } returns connection
        every { connection.isValid(any()) } returns true
        every { connection.close() } just Runs
    }

    @Nested
    @DisplayName("Scheduled Execution")
    inner class ScheduledExecution {

        @Test
        fun `should execute sync when enabled and database is ready`() = runTest {
            val successResult = SyncJobResult(
                success = true,
                totalPlans = 10,
                successfulPlans = 8,
                failedPlans = 2
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
            verify(exactly = 1) { dataSource.connection }
            verify(exactly = 1) { connection.isValid(3) }
        }

        @Test
        fun `should not execute sync when disabled`() {
            val disabledScheduler = PlanSyncScheduler(
                syncJobOrchestrator = syncJobOrchestrator,
                dataSource = dataSource,
                syncingEnabled = false
            )

            disabledScheduler.schedulePlanSync()

            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
            verify(exactly = 0) { dataSource.connection }
        }

        @Test
        fun `should not execute sync when database is not ready`() {
            every { connection.isValid(3) } returns false

            scheduler.schedulePlanSync()

            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
            verify(exactly = 1) { connection.isValid(3) }
        }

        @Test
        fun `should handle database connection failure gracefully`() {
            every { dataSource.connection } throws SQLException("Connection failed")

            scheduler.schedulePlanSync()

            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
            verify(exactly = 1) { dataSource.connection }
        }

        @Test
        fun `should handle database validation timeout`() {
            every { connection.isValid(3) } throws SQLException("Validation timeout")

            scheduler.schedulePlanSync()

            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
        }
    }

    @Nested
    @DisplayName("Concurrency Protection")
    inner class ConcurrencyProtection {

        @Test
        fun `should prevent overlapping executions`() = runTest {
            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean

            syncInProgress.set(true)

            scheduler.schedulePlanSync()

            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }

            assertTrue(syncInProgress.get())
        }

        @Test
        fun `should reset syncInProgress flag after successful execution`() = runTest {
            val successResult = SyncJobResult(success = true, totalPlans = 5, successfulPlans = 5)
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean

            scheduler.schedulePlanSync()

            assertFalse(syncInProgress.get())
            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should reset syncInProgress flag after failed execution`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws RuntimeException("Sync failed")

            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean

            scheduler.schedulePlanSync()

            assertFalse(syncInProgress.get())
            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should reset syncInProgress flag after database check failure`() {
            every { dataSource.connection } throws SQLException("Database unavailable")

            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean

            scheduler.schedulePlanSync()

            assertFalse(syncInProgress.get())
            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle concurrent access to syncInProgress flag`() = runTest {
            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean

            val initialState = syncInProgress.get()

            val firstCallSucceeded = syncInProgress.compareAndSet(false, true)

            val secondCallSucceeded = syncInProgress.compareAndSet(false, true)

            assertFalse(initialState)
            assertTrue(firstCallSucceeded)
            assertFalse(secondCallSucceeded)

            syncInProgress.set(false)
        }
    }

    @Nested
    @DisplayName("Database Readiness Check")
    inner class DatabaseReadinessCheck {

        @Test
        fun `should use correct timeout for database validation`() {
            every { connection.isValid(3) } returns true
            val successResult = SyncJobResult(success = true)
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            scheduler.schedulePlanSync()

            verify(exactly = 1) { connection.isValid(3) }
        }

        @Test
        fun `should close database connection properly`() {
            val successResult = SyncJobResult(success = true)
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            scheduler.schedulePlanSync()

            verify(exactly = 1) { connection.close() }
        }

        @Test
        fun `should close connection even when validation fails`() {
            every { connection.isValid(3) } returns false

            scheduler.schedulePlanSync()

            verify(exactly = 1) { connection.close() }
        }

        @Test
        fun `should handle connection close failure gracefully`() {
            // Set up connection.isValid to succeed first, then close() to fail
            every { connection.isValid(3) } returns true
            every { connection.close() } throws SQLException("Close failed")

            scheduler.schedulePlanSync()

            verify(exactly = 1) { connection.isValid(3) }
            verify(exactly = 1) { connection.close() }
            // Don't expect orchestrator to be called when close() fails
            coVerify(exactly = 0) { syncJobOrchestrator.orchestrateFullSync() }
        }
    }

    @Nested
    @DisplayName("Sync Result Handling")
    inner class SyncResultHandling {

        @Test
        fun `should handle successful sync result`() = runTest {
            val successResult = SyncJobResult(
                success = true,
                totalPlans = 15,
                successfulPlans = 12,
                failedPlans = 3,
                executionTimeMs = 2500
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle failed sync result`() = runTest {
            val failedResult = SyncJobResult(
                success = false,
                totalPlans = 0,
                successfulPlans = 0,
                failedPlans = 0,
                errors = listOf("Provider API unavailable", "Network timeout")
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns failedResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle zero plans scenario`() = runTest {
            val emptyResult = SyncJobResult(
                success = true,
                totalPlans = 0,
                successfulPlans = 0,
                failedPlans = 0
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns emptyResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle partial success scenario`() = runTest {
            val partialResult = SyncJobResult(
                success = true,
                totalPlans = 100,
                successfulPlans = 85,
                failedPlans = 15,
                executionTimeMs = 5000
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns partialResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }
    }

    @Nested
    @DisplayName("Exception Handling")
    inner class ExceptionHandling {

        @Test
        fun `should handle orchestrator exceptions gracefully`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws RuntimeException("Orchestrator failed")

            // Should not propagate exception
            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle timeout exceptions from orchestrator`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws java.util.concurrent.TimeoutException("Sync timeout")

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle interrupted exceptions`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws InterruptedException("Thread interrupted")

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle null pointer exceptions`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws NullPointerException("NPE in sync")

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle generic exceptions with null message`() = runTest {
            coEvery { syncJobOrchestrator.orchestrateFullSync() } throws RuntimeException(null as String?)

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }
    }

    @Nested
    @DisplayName("Configuration Integration")
    inner class ConfigurationIntegration {

        @Test
        fun `should respect sync enabled configuration`() {
            val enabledScheduler = PlanSyncScheduler(
                syncJobOrchestrator = syncJobOrchestrator,
                dataSource = dataSource,
                syncingEnabled = true
            )
            val disabledScheduler = PlanSyncScheduler(
                syncJobOrchestrator = syncJobOrchestrator,
                dataSource = dataSource,
                syncingEnabled = false
            )

            val successResult = SyncJobResult(success = true)
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            enabledScheduler.schedulePlanSync()
            disabledScheduler.schedulePlanSync()

            // Only enabled scheduler should execute
            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should work with different database connection types`() {
            // Test with different connection scenarios
            val alternateConnection = mockk<Connection>()
            every { alternateConnection.isValid(3) } returns true
            every { alternateConnection.close() } just Runs
            every { dataSource.connection } returns alternateConnection

            val successResult = SyncJobResult(success = true)
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns successResult

            scheduler.schedulePlanSync()

            verify(exactly = 1) { alternateConnection.isValid(3) }
            verify(exactly = 1) { alternateConnection.close() }
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        fun `should handle multiple rapid successive calls`() {
            val syncInProgress = ReflectionTestUtils.getField(scheduler, "syncInProgress") as AtomicBoolean
            val successResult = SyncJobResult(success = true)

            // Use a CountDownLatch or similar to control execution timing
            val syncStarted = java.util.concurrent.CountDownLatch(1)
            val allowSyncToComplete = java.util.concurrent.CountDownLatch(1)

            coEvery { syncJobOrchestrator.orchestrateFullSync() } coAnswers {
                syncStarted.countDown()
                allowSyncToComplete.await() // Wait for test to allow completion
                successResult
            }

            // Start first sync in background thread
            val job1 = kotlin.concurrent.thread {
                scheduler.schedulePlanSync()
            }

            // Wait for first sync to actually start
            syncStarted.await()

            scheduler.schedulePlanSync() // Should be skipped
            scheduler.schedulePlanSync() // Should be skipped

            // Allow first sync to complete
            allowSyncToComplete.countDown()
            job1.join()

            // Only one execution should have occurred
            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
            assertFalse(syncInProgress.get()) // Should be reset after completion
        }

        @Test
        fun `should handle very long sync operations`() = runTest {
            val longRunningSyncResult = SyncJobResult(
                success = true,
                totalPlans = 10000,
                successfulPlans = 9999,
                failedPlans = 1,
                executionTimeMs = 300000 // 5 minutes
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns longRunningSyncResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }

        @Test
        fun `should handle sync result with maximum integer values`() = runTest {
            val maxValueResult = SyncJobResult(
                success = true,
                totalPlans = Int.MAX_VALUE,
                successfulPlans = Int.MAX_VALUE - 1,
                failedPlans = 1,
                executionTimeMs = Long.MAX_VALUE
            )
            coEvery { syncJobOrchestrator.orchestrateFullSync() } returns maxValueResult

            scheduler.schedulePlanSync()

            coVerify(exactly = 1) { syncJobOrchestrator.orchestrateFullSync() }
        }
    }
}