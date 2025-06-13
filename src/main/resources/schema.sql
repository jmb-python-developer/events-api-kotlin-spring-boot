-- Event table (stores plan data from provider)
CREATE TABLE IF NOT EXISTS event (
    id VARCHAR(255) PRIMARY KEY,
    provider_event_id VARCHAR(255) NOT NULL UNIQUE, -- Maps to base_plan_id from provider
    title VARCHAR(255) NOT NULL,
    plan_start_date TIMESTAMP NOT NULL,             -- Maps to plan_start_date (when event happens)
    plan_end_date TIMESTAMP NOT NULL,               -- Maps to plan_end_date (when event ends)
    price_range_min DECIMAL(10,2) NOT NULL,
    price_range_max DECIMAL(10,2) NOT NULL,
    sell_mode VARCHAR(50) NOT NULL,
    organizer_company_id VARCHAR(255),
    sell_from TIMESTAMP,                            -- Maps to sell_from (when sales start)
    sell_to TIMESTAMP,                              -- Maps to sell_to (when sales end)
    sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    last_update TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

-- Zone table (unchanged structure)
CREATE TABLE IF NOT EXISTS zone (
    zone_id VARCHAR(255) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    capacity INTEGER NOT NULL,
    numbered BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

-- Indexes optimized for plan queries
CREATE INDEX IF NOT EXISTS idx_event_provider_id ON event(provider_event_id);
CREATE INDEX IF NOT EXISTS idx_event_plan_dates ON event(plan_start_date, plan_end_date);
CREATE INDEX IF NOT EXISTS idx_event_sell_dates ON event(sell_from, sell_to);
CREATE INDEX IF NOT EXISTS idx_zone_event_id ON zone(event_id);
CREATE INDEX IF NOT EXISTS idx_event_sell_mode ON event(sell_mode);
CREATE INDEX IF NOT EXISTS idx_event_pricing ON event(price_range_min, price_range_max);
