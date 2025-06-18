package com.jmb.events_api.query.infrastructure.integration

import com.jmb.events_api.sync.infrastructure.persistence.PlanJpaEntity
import com.jmb.events_api.sync.infrastructure.persistence.PlanJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

/**
 * Integration test for the Query module - End-to-end testing from Controller to Database
 * Tests the complete query flow: REST Controller → Application Service → Query Port → Database
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
    ]
)
@Transactional
@DisplayName("Query Module Integration Tests - End to End")
class QueryModuleIntegrationTest {

    @Autowired
    private lateinit var webApplicationContext: WebApplicationContext

    @Autowired
    private lateinit var planRepository: PlanJpaRepository

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        // Set up MockMvc
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()

        // Clean database and set up test data
        planRepository.deleteAll()
        setupTestData()
    }

    private fun setupTestData() {
        val now = Instant.now()

        val testPlans = listOf(
            PlanJpaEntity(
                id = "plan-1",
                providerPlanId = "provider-001",
                title = "Summer Concert 2024",
                planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
                priceRangeMin = BigDecimal("25.00"),
                priceRangeMax = BigDecimal("75.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "music-events-inc",
                sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
                sellTo = LocalDateTime.of(2024, 6, 15, 18, 0),
                soldOut = false,
                lastUpdated = now,
                version = 1
            ),
            PlanJpaEntity(
                id = "plan-2",
                providerPlanId = "provider-002",
                title = "Jazz Night",
                planStartDate = LocalDateTime.of(2024, 6, 20, 19, 30),
                planEndDate = LocalDateTime.of(2024, 6, 20, 22, 30),
                priceRangeMin = BigDecimal("15.00"),
                priceRangeMax = BigDecimal("45.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "jazz-club",
                sellFrom = LocalDateTime.of(2024, 5, 15, 0, 0),
                sellTo = LocalDateTime.of(2024, 6, 20, 17, 0),
                soldOut = false,
                lastUpdated = now,
                version = 1
            )
        )

        planRepository.saveAll(testPlans)
    }

    @Test
    fun `should return successful response for valid search request`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2024-06-01")
                .param("ends_at", "2024-06-30")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.events").isArray)
    }

    @Test
    fun `should return bad request for missing required parameters`() {
        mockMvc.perform(get("/search"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `should handle invalid date format`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "invalid-date")
                .param("ends_at", "2024-06-30")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `should verify database integration works`() {
        // Verify test data exists
        val plansInDb = planRepository.findAll()
        assert(plansInDb.size == 2) { "Expected 2 test plans in database, found ${plansInDb.size}" }

        // Verify API responds
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2024-06-01")
                .param("ends_at", "2024-06-30")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun `should handle availableOnly parameter`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2024-06-01")
                .param("ends_at", "2024-06-30")
                .param("availableOnly", "true")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.events").isArray)
    }

    @Test
    fun `should handle maxResults parameter`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2024-06-01")
                .param("ends_at", "2024-06-30")
                .param("maxResults", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.events").isArray)
    }

    @Test
    fun `should return empty results for future date range`() {
        mockMvc.perform(
            get("/search")
                .param("starts_at", "2025-01-01")
                .param("ends_at", "2025-01-31")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.events").isArray)
            .andExpect(jsonPath("$.data.events.length()").value(0))
    }
}