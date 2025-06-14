package com.jmb.events_api.sync.infrastructure.persistence

import com.jmb.events_api.sync.domain.model.Plan
import com.jmb.events_api.sync.domain.repository.PlanRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class PlanRepositoryAdapter(
    private val planJpaRepository: PlanJpaRepository,
    private val planEntityMapper: PlanEntityMapper,
    private val zoneEntityMapper: ZoneEntityMapper,
): PlanRepository {

    private val logger = LoggerFactory.getLogger(PlanRepositoryAdapter::class.java)

    override fun upsertPlan(plan: Plan): Plan {
        // Check if entity exists
        val existingEntity = planJpaRepository.findByProviderPlanId(plan.providerPlanId)

        val savedEntity = if (existingEntity != null) {
            val entityToUpdate = planEntityMapper.toEntityForUpdate(plan, existingEntity)
            setupZones(entityToUpdate, plan)
            planJpaRepository.save(entityToUpdate)
        } else {
            val entityToSave = planEntityMapper.toEntity(plan)
            setupZones(entityToSave, plan)
            planJpaRepository.save(entityToSave)
        }

        logger.debug("Upserted plan: ${plan.title} with ${savedEntity.zones.size} zones")
        return planEntityMapper.toDomain(savedEntity)
    }

    private fun setupZones(entity: PlanJpaEntity, plan: Plan) {
        entity.zones.clear()
        val zones = plan.zones.mapIndexed {index, zone ->
            val zoneEntity = zoneEntityMapper.toEntity(zone, entity.id, index)
            zoneEntity.plan = entity
            zoneEntity
        }
        entity.zones.addAll(zones)
    }

    @Transactional(readOnly = true)
    override fun findByProviderId(providerId: String): Plan? {
        val entity = planJpaRepository.findByProviderPlanId(providerId)
        return entity?.let { planEntityMapper.toDomain(it) }
    }
}
