#!/bin/bash

# Performance Testing Script for Events API
# Tests the /search endpoint with various scenarios and measures response times
# Note: API still serves "events" but is now backed by "plans" internally

set -e

# Configuration
BASE_URL="http://localhost:8080"
ENDPOINT="/search"
OUTPUT_DIR="./performance-results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo -e "${BLUE}🚀 Events API Performance Testing Script${NC}"
echo "================================================="
echo "Base URL: $BASE_URL"
echo "Timestamp: $TIMESTAMP"
echo "Results will be saved to: $OUTPUT_DIR"
echo "Note: API serves events data backed by plans internally"
echo ""

# ... rest of the script remains the same since it's testing the public API
# which still uses "events" terminology for backward compatibility

-- File 5: SyncFailedEvent.kt (update to use plan terminology)
package com.jmb.events_api.sync.domain.event

import com.jmb.events_api.shared.domain.event.DomainEvent
import com.jmb.events_api.shared.domain.event.EventType
import java.time.Instant
import java.util.UUID

data class SyncFailedEvent(
    val providerPlanId: String,        // Updated from providerEventId
    val failureReason: String,
    val failedAt: Instant = Instant.now(),
    override val eventId: String = UUID.randomUUID().toString(),
    override val occurredAt: Instant = Instant.now(),
    override val eventType: String = EventType.PLAN_SYNC_FAILED.value,
): DomainEvent

-- File 6: DatabaseConfig.kt (update package scan path if needed)
package com.jmb.events_api.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement

@Configuration
@EnableJpaRepositories(basePackages = ["com.jmb.events_api"])
class DatabaseConfig

-- File 7: Migration script (optional - for existing databases)
-- migration.sql (if you have existing data to migrate)
-- This would be used to migrate from the old EVENT table to new PLAN table

-- Step 1: Create new plan table (if schema.sql wasn't run)
-- (schema.sql content would go here)

-- Step 2: Migrate data from old event table to plan table (if you had existing data)
-- INSERT INTO plan (
--     id, provider_plan_id, title, plan_start_date, plan_end_date,
--     price_range_min, price_range_max, sell_mode, organizer_company_id,
--     sell_from, sell_to, sold_out, last_update, version
-- )
-- SELECT
--     id, provider_event_id, title, plan_start_date, plan_end_date,
--     price_range_min, price_range_max, sell_mode, organizer_company_id,
--     sell_from, sell_to, sold_out, last_update, version
-- FROM event;

-- Step 3: Update zone table to reference plan_id
-- UPDATE zone SET plan_id = event_id;
-- ALTER TABLE zone DROP FOREIGN KEY fk_zone_event;
-- ALTER TABLE zone DROP COLUMN event_id;
-- ALTER TABLE zone ADD CONSTRAINT fk_zone_plan FOREIGN KEY (plan_id) REFERENCES plan(id);

-- Step 4: Drop old event table
-- DROP TABLE event;