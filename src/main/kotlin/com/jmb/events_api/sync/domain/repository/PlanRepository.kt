package com.jmb.events_api.sync.domain.repository

import com.jmb.events_api.sync.domain.model.Plan

interface PlanRepository {
    fun upsertPlan(plan: Plan): Plan
    fun findByProviderId(providerId: String): Plan?
}
