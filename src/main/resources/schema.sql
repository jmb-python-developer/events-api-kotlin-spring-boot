-- Event table
CREATE TABLE IF NOT EXISTS event (
    id VARCHAR(255) PRIMARY KEY,
    provider_event_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    sell_from TIMESTAMP NOT NULL,
    sell_to TIMESTAMP NOT NULL,
    price_range_min DECIMAL(10,2) NOT NULL,
    price_range_max DECIMAL(10,2) NOT NULL,
    sell_mode VARCHAR(50) NOT NULL,
    organizer_company_id VARCHAR(255),
    sell_period_from TIMESTAMP,
    sell_period_to TIMESTAMP,
    sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    last_update TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 1
);

-- Zone table
CREATE TABLE IF NOT EXISTS zone (
    zone_id VARCHAR(255) PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    capacity INTEGER NOT NULL,
    numbered BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (event_id) REFERENCES event(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_event_provider_id ON event(provider_event_id);
CREATE INDEX IF NOT EXISTS idx_event_sell_dates ON event(sell_from, sell_to);
CREATE INDEX IF NOT EXISTS idx_zone_event_id ON zone(event_id);
-- Nice to have ones, might not be used often
CREATE INDEX IF NOT EXISTS idx_event_sell_mode ON event(sell_mode);
CREATE INDEX IF NOT EXISTS idx_event_pricing ON event(price_range_min, price_range_max);