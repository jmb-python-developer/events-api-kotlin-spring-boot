INSERT INTO plan (
    id, provider_plan_id, title,
    plan_start_date, plan_end_date,        -- When the show happens
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_from, sell_to,                    -- When tickets are sold
    sold_out, last_update, version
) VALUES (
    'plan-291-demo', '291', 'Camela en concierto',
    '2021-06-30 21:00:00', '2021-06-30 22:00:00',  -- Show is 21:00-22:00
    15.00, 30.00,
    'ONLINE', NULL,
    '2020-07-01 00:00:00', '2021-06-30 20:00:00',  -- Sales period
    false, CURRENT_TIMESTAMP, 1
);

-- Plan 2: Los Morancos (base_plan_id="1591")
INSERT INTO plan (
    id, provider_plan_id, title,
    plan_start_date, plan_end_date,        -- When the show happens
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_from, sell_to,                    -- When tickets are sold
    sold_out, last_update, version
) VALUES (
    'plan-1591-demo', '1591', 'Los Morancos',
    '2021-07-31 20:00:00', '2021-07-31 21:20:00',  -- Show is 20:00-21:20
    65.00, 75.00,
    'ONLINE', '1',
    '2021-06-26 00:00:00', '2021-07-31 19:50:00',  -- Sales period
    false, CURRENT_TIMESTAMP, 1
);

-- Plan 3: Tributo a Juanito Valderrama (base_plan_id="444") - OFFLINE, won't show in results
INSERT INTO plan (
    id, provider_plan_id, title,
    plan_start_date, plan_end_date,        -- When the show happens
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_from, sell_to,                    -- When tickets are sold
    sold_out, last_update, version
) VALUES (
    'plan-444-demo', '444', 'Tributo a Juanito Valderrama',
    '2021-09-31 20:00:00', '2021-09-31 21:00:00',  -- Show is 20:00-21:00
    65.00, 65.00,
    'OFFLINE', '1',
    '2021-02-10 00:00:00', '2021-09-31 19:50:00',  -- Sales period
    false, CURRENT_TIMESTAMP, 1
);

-- Plan 4: Future Plan for Testing Date Ranges
INSERT INTO plan (
    id, provider_plan_id, title,
    plan_start_date, plan_end_date,        -- When the show happens
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_from, sell_to,                    -- When tickets are sold
    sold_out, last_update, version
) VALUES (
    'plan-future-demo', 'FUTURE-PLAN-001', 'Future Concert Plan - Tech Demo',
    '2025-12-31 20:00:00', '2025-12-31 23:00:00',  -- Show is 20:00-23:00
    25.00, 100.00,
    'ONLINE', '3',
    '2025-06-01 00:00:00', '2025-12-31 19:00:00',  -- Sales period
    false, CURRENT_TIMESTAMP, 1
);

-- Zones updated to reference plan_id
INSERT INTO zone (zone_id, plan_id, name, price, capacity, numbered) VALUES
('40', 'plan-291-demo', 'Platea', 20.00, 240, true),
('38', 'plan-291-demo', 'Grada 2', 15.00, 50, false),
('30', 'plan-291-demo', 'A28', 30.00, 90, true);

INSERT INTO zone (zone_id, plan_id, name, price, capacity, numbered) VALUES
('186-1', 'plan-1591-demo', 'Amfiteatre', 75.00, 0, true),
('186-2', 'plan-1591-demo', 'Amfiteatre', 65.00, 14, false);

INSERT INTO zone (zone_id, plan_id, name, price, capacity, numbered) VALUES
('7', 'plan-444-demo', 'Amfiteatre', 65.00, 22, false);

INSERT INTO zone (zone_id, plan_id, name, price, capacity, numbered) VALUES
('VIP-PLAN-001', 'plan-future-demo', 'VIP Section', 100.00, 50, true),
('GA-PLAN-001', 'plan-future-demo', 'General Admission', 25.00, 500, false),
('BAL-PLAN-001', 'plan-future-demo', 'Balcony', 60.00, 100, true);