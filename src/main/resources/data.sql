-- Sample Events Data based on the XML provider structure
-- These mirror the events from the XML examples you showed earlier

-- Event 1: Camela en concierto (base_event_id="291")
INSERT INTO event (
    id, provider_event_id, title,
    sell_from, sell_to,
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_period_from, sell_period_to,
    sold_out, last_update, version
) VALUES (
    'evt-291-demo', '291', 'Camela en concierto',
    '2021-06-30 21:00:00', '2021-06-30 22:00:00',
    15.00, 30.00,
    'ONLINE', NULL,
    '2020-07-01 00:00:00', '2021-06-30 20:00:00',
    false, CURRENT_TIMESTAMP, 1
);

-- Event 2: Pantomima Full (base_event_id="322")
INSERT INTO event (
    id, provider_event_id, title,
    sell_from, sell_to,
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_period_from, sell_period_to,
    sold_out, last_update, version
) VALUES (
    'evt-322-demo', '322', 'Pantomima Full',
    '2021-02-10 20:00:00', '2021-02-10 21:30:00',
    55.00, 55.00,
    'ONLINE', '2',
    '2021-01-01 00:00:00', '2021-02-09 19:50:00',
    false, CURRENT_TIMESTAMP, 1
);

-- Event 3: Los Morancos (base_event_id="1591")
INSERT INTO event (
    id, provider_event_id, title,
    sell_from, sell_to,
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_period_from, sell_period_to,
    sold_out, last_update, version
) VALUES (
    'evt-1591-demo', '1591', 'Los Morancos',
    '2021-07-31 20:00:00', '2021-07-31 21:00:00',
    65.00, 75.00,
    'ONLINE', '1',
    '2021-06-26 00:00:00', '2021-07-31 19:50:00',
    false, CURRENT_TIMESTAMP, 1
);

-- Event 4: Future Event for Testing Date Ranges
INSERT INTO event (
    id, provider_event_id, title,
    sell_from, sell_to,
    price_range_min, price_range_max,
    sell_mode, organizer_company_id,
    sell_period_from, sell_period_to,
    sold_out, last_update, version
) VALUES (
    'evt-future-demo', 'FUTURE-001', 'Future Concert - Tech Demo',
    '2025-12-31 20:00:00', '2025-12-31 23:00:00',
    25.00, 100.00,
    'ONLINE', '3',
    '2025-06-01 00:00:00', '2025-12-31 19:00:00',
    false, CURRENT_TIMESTAMP, 1
);

-- Zones for Event 1: Camela (3 zones like in XML)
INSERT INTO zone (zone_id, event_id, name, price, capacity, numbered) VALUES
('40', 'evt-291-demo', 'Platea', 20.00, 243, true),
('38', 'evt-291-demo', 'Grada 2', 15.00, 100, false),
('30', 'evt-291-demo', 'A28', 30.00, 90, true);

-- Zones for Event 2: Pantomima (1 zone like in XML)
INSERT INTO zone (zone_id, event_id, name, price, capacity, numbered) VALUES
('311', 'evt-322-demo', 'A42', 55.00, 2, true);

-- Zones for Event 3: Los Morancos (2 zones like in XML)
INSERT INTO zone (zone_id, event_id, name, price, capacity, numbered) VALUES
('186-1', 'evt-1591-demo', 'Amfiteatre VIP', 75.00, 2, true),
('186-2', 'evt-1591-demo', 'Amfiteatre General', 65.00, 16, false);

-- Zones for Event 4: Future Event (varied pricing)
INSERT INTO zone (zone_id, event_id, name, price, capacity, numbered) VALUES
('VIP-001', 'evt-future-demo', 'VIP Section', 100.00, 50, true),
('GA-001', 'evt-future-demo', 'General Admission', 25.00, 500, false),
('BAL-001', 'evt-future-demo', 'Balcony', 60.00, 100, true);