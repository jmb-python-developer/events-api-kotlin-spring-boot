package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime

@DisplayName("PlanEntityMapper Infrastructure Tests")
class PlanEntityMapperTest {

    private val zoneEntityMapper = mockk<ZoneEntityMapper>()
    private val mapper = PlanEntityMapper(zoneEntityMapper)

    @BeforeEach
    fun setUp() {
        clearAllMocks()
    }

    private fun createTestDomainPlan(
        id: String = "test-plan-id",
        providerPlanId: String = "provider-123",
        title: String = "Test Concert",
        version: Long = 1,
        soldOut: Boolean = false,
        organizerCompanyId: String? = "company-1"
    ): Plan {
        val zone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
        every { zoneEntityMapper.toDomain(any()) } returns zone

        return Plan.createOnline(
            id = PlanId.of(id),
            title = title,
            date = DateRange(
                from = LocalDateTime.of(2024, 6, 15, 20, 0),
                to = LocalDateTime.of(2024, 6, 15, 23, 0)
            ),
            priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("50.00")),
            providerPlanId = providerPlanId,
            organizerCompanyId = organizerCompanyId,
            sellPeriod = DateRange(
                from = LocalDateTime.of(2024, 5, 1, 0, 0),
                to = LocalDateTime.of(2024, 6, 15, 19, 0)
            ),
            soldOut = soldOut,
            zones = listOf(zone),
            version = version
        )
    }

    private fun createTestJpaEntity(
        id: String = "test-plan-id",
        providerPlanId: String = "provider-123",
        title: String = "Test Concert",
        version: Long = 1,
        soldOut: Boolean = false,
        organizerCompanyId: String? = "company-1"
    ): PlanJpaEntity {
        val entity = PlanJpaEntity(
            id = id,
            providerPlanId = providerPlanId,
            title = title,
            planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
            planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
            priceRangeMin = BigDecimal("20.00"),
            priceRangeMax = BigDecimal("50.00"),
            sellMode = "ONLINE",
            organizerCompanyId = organizerCompanyId,
            sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0),
            sellTo = LocalDateTime.of(2024, 6, 15, 19, 0),
            soldOut = soldOut,
            lastUpdated = Instant.parse("2024-06-01T10:00:00Z"),
            version = version
        )

        val zoneEntity = ZoneJpaEntity("zone-1", "General", BigDecimal("25.00"), 100, false)
        zoneEntity.plan = entity
        entity.zones.add(zoneEntity)

        return entity
    }

    @Nested
    @DisplayName("Domain to Entity Mapping")
    inner class DomainToEntityMapping {

        @Test
        fun `should map domain plan to JPA entity with all fields`() {
            val domainPlan = createTestDomainPlan()

            val entity = mapper.toEntity(domainPlan)

            assertEquals(domainPlan.id.value, entity.id)
            assertEquals(domainPlan.providerPlanId, entity.providerPlanId)
            assertEquals(domainPlan.title, entity.title)
            assertEquals(domainPlan.date.from, entity.planStartDate)
            assertEquals(domainPlan.date.to, entity.planEndDate)
            assertEquals(domainPlan.priceRange.min, entity.priceRangeMin)
            assertEquals(domainPlan.priceRange.max, entity.priceRangeMax)
            assertEquals(domainPlan.sellMode.name, entity.sellMode)
            assertEquals(domainPlan.organizerCompanyId, entity.organizerCompanyId)
            assertEquals(domainPlan.sellPeriod?.from, entity.sellFrom)
            assertEquals(domainPlan.sellPeriod?.to, entity.sellTo)
            assertEquals(domainPlan.soldOut, entity.soldOut)
            assertEquals(domainPlan.lastUpdated, entity.lastUpdated)
            assertEquals(domainPlan.version, entity.version)
        }

        @Test
        fun `should handle null organizer company ID`() {
            val domainPlan = createTestDomainPlan(organizerCompanyId = null)

            val entity = mapper.toEntity(domainPlan)

            assertNull(entity.organizerCompanyId)
        }

        @Test
        fun `should handle null sell period`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            val domainPlan = Plan.createOnline(
                id = PlanId.of("test-id"),
                title = "Test Concert",
                date = DateRange(
                    from = LocalDateTime.of(2024, 6, 15, 20, 0),
                    to = LocalDateTime.of(2024, 6, 15, 23, 0)
                ),
                priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("50.00")),
                providerPlanId = "provider-123",
                sellPeriod = null, // No sell period
                zones = listOf(zone)
            )

            val entity = mapper.toEntity(domainPlan)

            assertNull(entity.sellFrom)
            assertNull(entity.sellTo)
        }

        @Test
        fun `should map sold out status correctly`() {
            val soldOutPlan = createTestDomainPlan(soldOut = true)
            val availablePlan = createTestDomainPlan(soldOut = false)

            val soldOutEntity = mapper.toEntity(soldOutPlan)
            val availableEntity = mapper.toEntity(availablePlan)

            assertTrue(soldOutEntity.soldOut)
            assertTrue(!availableEntity.soldOut)
        }

        @Test
        fun `should preserve price precision`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.99"), 100, false)
            val domainPlan = Plan.createOnline(
                id = PlanId.of("test-id"),
                title = "Test Concert",
                date = DateRange(
                    from = LocalDateTime.of(2024, 6, 15, 20, 0),
                    to = LocalDateTime.of(2024, 6, 15, 23, 0)
                ),
                priceRange = PriceRange(BigDecimal("19.99"), BigDecimal("59.99")),
                providerPlanId = "provider-123",
                zones = listOf(zone)
            )

            val entity = mapper.toEntity(domainPlan)

            assertEquals(0, BigDecimal("19.99").compareTo(entity.priceRangeMin))
            assertEquals(0, BigDecimal("59.99").compareTo(entity.priceRangeMax))
        }
    }

    @Nested
    @DisplayName("Entity to Domain Mapping")
    inner class EntityToDomainMapping {

        @Test
        fun `should map JPA entity to domain plan with all fields`() {
            val jpaEntity = createTestJpaEntity()
            val testZone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            every { zoneEntityMapper.toDomain(any()) } returns testZone

            val domainPlan = mapper.toDomain(jpaEntity)

            assertEquals(jpaEntity.id, domainPlan.id.value)
            assertEquals(jpaEntity.providerPlanId, domainPlan.providerPlanId)
            assertEquals(jpaEntity.title, domainPlan.title)
            assertEquals(jpaEntity.planStartDate, domainPlan.date.from)
            assertEquals(jpaEntity.planEndDate, domainPlan.date.to)
            assertEquals(jpaEntity.priceRangeMin, domainPlan.priceRange.min)
            assertEquals(jpaEntity.priceRangeMax, domainPlan.priceRange.max)
            assertEquals(SellMode.ONLINE, domainPlan.sellMode)
            assertEquals(jpaEntity.organizerCompanyId, domainPlan.organizerCompanyId)
            assertEquals(jpaEntity.sellFrom, domainPlan.sellPeriod?.from)
            assertEquals(jpaEntity.sellTo, domainPlan.sellPeriod?.to)
            assertEquals(jpaEntity.soldOut, domainPlan.soldOut)
            assertEquals(jpaEntity.lastUpdated, domainPlan.lastUpdated)
            assertEquals(jpaEntity.version, domainPlan.version)
        }

        @Test
        fun `should handle null organizer company ID in entity`() {
            val jpaEntity = createTestJpaEntity(organizerCompanyId = null)
            val testZone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            every { zoneEntityMapper.toDomain(any()) } returns testZone

            val domainPlan = mapper.toDomain(jpaEntity)

            assertNull(domainPlan.organizerCompanyId)
        }

        @Test
        fun `should handle null sell period in entity`() {
            val jpaEntity = PlanJpaEntity(
                id = "test-id",
                providerPlanId = "provider-123",
                title = "Test Concert",
                planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
                priceRangeMin = BigDecimal("20.00"),
                priceRangeMax = BigDecimal("50.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "company-1",
                sellFrom = null, // No sell period
                sellTo = null,   // No sell period
                soldOut = false,
                lastUpdated = Instant.now(),
                version = 1
            )
            val testZone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            every { zoneEntityMapper.toDomain(any()) } returns testZone

            val domainPlan = mapper.toDomain(jpaEntity)

            assertNull(domainPlan.sellPeriod)
        }

        @Test
        fun `should handle partial sell period data gracefully`() {
            val jpaEntity = PlanJpaEntity(
                id = "test-id",
                providerPlanId = "provider-123",
                title = "Test Concert",
                planStartDate = LocalDateTime.of(2024, 6, 15, 20, 0),
                planEndDate = LocalDateTime.of(2024, 6, 15, 23, 0),
                priceRangeMin = BigDecimal("20.00"),
                priceRangeMax = BigDecimal("50.00"),
                sellMode = "ONLINE",
                organizerCompanyId = "company-1",
                sellFrom = LocalDateTime.of(2024, 5, 1, 0, 0), // Only sellFrom
                sellTo = null, // Missing sellTo
                soldOut = false,
                lastUpdated = Instant.now(),
                version = 1
            )
            val testZone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            every { zoneEntityMapper.toDomain(any()) } returns testZone

            val domainPlan = mapper.toDomain(jpaEntity)

            assertNull(domainPlan.sellPeriod) // Should be null if incomplete
        }

        @Test
        fun `should map zones correctly`() {
            val jpaEntity = createTestJpaEntity()
            val testZone1 = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            val testZone2 = Zone("zone-2", "VIP", BigDecimal("75.00"), 50, true)

            // Add second zone to entity
            val zoneEntity2 = ZoneJpaEntity("zone-2", "VIP", BigDecimal("75.00"), 50, true)
            zoneEntity2.plan = jpaEntity
            jpaEntity.zones.add(zoneEntity2)

            every { zoneEntityMapper.toDomain(jpaEntity.zones[0]) } returns testZone1
            every { zoneEntityMapper.toDomain(jpaEntity.zones[1]) } returns testZone2

            val domainPlan = mapper.toDomain(jpaEntity)

            assertEquals(2, domainPlan.zones.size)
            verify(exactly = 2) { zoneEntityMapper.toDomain(any()) }
        }
    }

    @Nested
    @DisplayName("Update Entity Mapping")
    inner class UpdateEntityMapping {

        @Test
        fun `toEntityForUpdate should preserve existing entity ID and providerPlanId`() {
            val domainPlan = createTestDomainPlan(
                id = "new-domain-id", // Different from existing
                providerPlanId = "new-provider-id", // Different from existing
                title = "Updated Title"
            )
            val existingEntity = createTestJpaEntity(
                id = "existing-entity-id",
                providerPlanId = "existing-provider-id",
                title = "Original Title"
            )

            val updatedEntity = mapper.toEntityForUpdate(domainPlan, existingEntity)

            // Should preserve existing entity's identity
            assertEquals("existing-entity-id", updatedEntity.id)
            assertEquals("existing-provider-id", updatedEntity.providerPlanId)
            // But update the title from domain
            assertEquals("Updated Title", updatedEntity.title)
        }

        @Test
        fun `toEntityForUpdate should preserve existing version`() {
            val domainPlan = createTestDomainPlan(version = 5)
            val existingEntity = createTestJpaEntity(version = 3)

            val updatedEntity = mapper.toEntityForUpdate(domainPlan, existingEntity)

            assertEquals(3, updatedEntity.version) // Should preserve existing version
        }

        @Test
        fun `toEntityForUpdate should update all mutable fields from domain`() {
            val domainPlan = createTestDomainPlan(
                title = "Updated Concert",
                soldOut = true,
                organizerCompanyId = "new-company"
            )
            val existingEntity = createTestJpaEntity(
                title = "Original Concert",
                soldOut = false,
                organizerCompanyId = "old-company"
            )

            val updatedEntity = mapper.toEntityForUpdate(domainPlan, existingEntity)

            assertEquals("Updated Concert", updatedEntity.title)
            assertTrue(updatedEntity.soldOut)
            assertEquals("new-company", updatedEntity.organizerCompanyId)
            assertEquals(domainPlan.priceRange.min, updatedEntity.priceRangeMin)
            assertEquals(domainPlan.priceRange.max, updatedEntity.priceRangeMax)
            assertEquals(domainPlan.date.from, updatedEntity.planStartDate)
            assertEquals(domainPlan.date.to, updatedEntity.planEndDate)
        }

        @Test
        fun `toEntityForUpdate should handle null values correctly`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            val domainPlan = Plan.createOnline(
                id = PlanId.of("domain-id"),
                title = "Updated Concert",
                date = DateRange(
                    from = LocalDateTime.of(2024, 6, 15, 20, 0),
                    to = LocalDateTime.of(2024, 6, 15, 23, 0)
                ),
                priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("50.00")),
                providerPlanId = "provider-123",
                organizerCompanyId = null, // Null in domain
                sellPeriod = null, // Null in domain
                zones = listOf(zone)
            )
            val existingEntity = createTestJpaEntity(
                organizerCompanyId = "existing-company"
            )

            val updatedEntity = mapper.toEntityForUpdate(domainPlan, existingEntity)

            assertNull(updatedEntity.organizerCompanyId)
            assertNull(updatedEntity.sellFrom)
            assertNull(updatedEntity.sellTo)
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping Consistency")
    inner class BidirectionalMappingConsistency {

        @Test
        fun `should maintain data consistency in round-trip domain to entity to domain`() {
            val originalDomain = createTestDomainPlan()
            val testZone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            every { zoneEntityMapper.toDomain(any()) } returns testZone

            val entity = mapper.toEntity(originalDomain)
            val reconstructedDomain = mapper.toDomain(entity)

            assertEquals(originalDomain.id.value, reconstructedDomain.id.value)
            assertEquals(originalDomain.providerPlanId, reconstructedDomain.providerPlanId)
            assertEquals(originalDomain.title, reconstructedDomain.title)
            assertEquals(originalDomain.sellMode, reconstructedDomain.sellMode)
            assertEquals(originalDomain.soldOut, reconstructedDomain.soldOut)
            assertEquals(originalDomain.organizerCompanyId, reconstructedDomain.organizerCompanyId)
            assertEquals(originalDomain.version, reconstructedDomain.version)
        }

        @Test
        fun `should handle precision loss gracefully in price mapping`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.999"), 100, false)
            val domainPlan = Plan.createOnline(
                id = PlanId.of("test-id"),
                title = "Test Concert",
                date = DateRange(
                    from = LocalDateTime.of(2024, 6, 15, 20, 0),
                    to = LocalDateTime.of(2024, 6, 15, 23, 0)
                ),
                priceRange = PriceRange(BigDecimal("19.999"), BigDecimal("59.999")),
                providerPlanId = "provider-123",
                zones = listOf(zone)
            )

            every { zoneEntityMapper.toDomain(any()) } returns zone

            val entity = mapper.toEntity(domainPlan)
            val reconstructedDomain = mapper.toDomain(entity)

            // Prices should maintain reasonable precision
            assertEquals(0, domainPlan.priceRange.min.compareTo(reconstructedDomain.priceRange.min))
            assertEquals(0, domainPlan.priceRange.max.compareTo(reconstructedDomain.priceRange.max))
        }

        @Test
        fun `should handle edge case datetime values correctly`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            val minDateTime = LocalDateTime.of(1970, 1, 1, 0, 0, 0)
            val maxDateTime = LocalDateTime.of(2099, 12, 31, 23, 59, 59)

            val domainPlan = Plan.createOnline(
                id = PlanId.of("test-id"),
                title = "Edge Case Concert",
                date = DateRange(from = minDateTime, to = maxDateTime),
                priceRange = PriceRange(BigDecimal("0.01"), BigDecimal("9999.99")),
                providerPlanId = "provider-123",
                sellPeriod = DateRange(from = minDateTime, to = maxDateTime),
                zones = listOf(zone)
            )

            every { zoneEntityMapper.toDomain(any()) } returns zone

            val entity = mapper.toEntity(domainPlan)
            val reconstructedDomain = mapper.toDomain(entity)

            assertEquals(minDateTime, reconstructedDomain.date.from)
            assertEquals(maxDateTime, reconstructedDomain.date.to)
            assertEquals(minDateTime, reconstructedDomain.sellPeriod?.from)
            assertEquals(maxDateTime, reconstructedDomain.sellPeriod?.to)
        }
    }

    @Nested
    @DisplayName("Error Scenarios")
    inner class ErrorScenarios {

        @Test
        fun `should handle empty zones list`() {
            val zone = Zone("zone-1", "General", BigDecimal("25.00"), 100, false)
            val domainPlan = Plan.createOnline(
                id = PlanId.of("test-id"),
                title = "Test Concert",
                date = DateRange(
                    from = LocalDateTime.of(2024, 6, 15, 20, 0),
                    to = LocalDateTime.of(2024, 6, 15, 23, 0)
                ),
                priceRange = PriceRange(BigDecimal("20.00"), BigDecimal("50.00")),
                providerPlanId = "provider-123",
                zones = listOf(zone) // Domain requires at least one zone
            )

            val entity = mapper.toEntity(domainPlan)

            assertEquals("test-id", entity.id)
            assertEquals("Test Concert", entity.title)
        }

        @Test
        fun `should handle entity with empty zones list`() {
            val jpaEntity = createTestJpaEntity()
            jpaEntity.zones.clear() // Empty zones

            every { zoneEntityMapper.toDomain(any()) } returns Zone("default", "Default", BigDecimal("0"), 0, false)

            val domainPlan = mapper.toDomain(jpaEntity)

            assertTrue(domainPlan.zones.isEmpty())
        }
    }
}