-- Sample lots around Natal (RN), loaded only with the demo Spring profile.
--
-- Repeatable (R__) instead of versioned: Flyway runs it again whenever this file changes, and the
-- API can still start without the demo profile on the same database (see application.yml).
-- Fixed ids make a second run update the rows already there instead of duplicating them.
--
-- Rings are counterclockwise (RFC 7946) and in longitude, latitude order. The two Ponta Negra lots
-- share the edge at longitude -35.1980 exactly: neighbors that touch without overlapping, which the
-- API accepts.

-- Demo account: demo@example.com / demo1234. The hash is BCrypt (cost 10) of that password,
-- precomputed so the seed needs no database extension.
INSERT INTO app_user (id, name, email, password_hash)
VALUES ('b0000000-0000-4000-8000-000000000001', 'Demo Seller', 'demo@example.com',
        '$2a$10$hE4rc.15nporyweextjMA.braCr6qcnRT3X4uppVzj8VUOOEFkTCK')
ON CONFLICT (id) DO NOTHING;

INSERT INTO land_lot (id, title, description, price, contact_name, contact_phone, contact_email, geom, status, owner_id)
VALUES
    ('a0000000-0000-4000-8000-000000000001',
     'Ponta Negra lot, west side',
     'Flat lot a few blocks from the beach, ready to build. Shares its east border with the lot next door.',
     480000.00, 'Ana Souza', '(84) 90000-0001', NULL,
     ST_GeomFromText('POLYGON((-35.1995 -5.8815, -35.1980 -5.8815, -35.1980 -5.8800, -35.1995 -5.8800, -35.1995 -5.8815))', 4326),
     'AVAILABLE', 'b0000000-0000-4000-8000-000000000001'),
    ('a0000000-0000-4000-8000-000000000002',
     'Ponta Negra lot, east side',
     'Corner lot with paved access on two streets. Can be bought together with the lot on its west border.',
     450000.00, 'Bruno Lima', NULL, 'bruno.lima@example.com',
     ST_GeomFromText('POLYGON((-35.1980 -5.8815, -35.1966 -5.8815, -35.1966 -5.8800, -35.1980 -5.8800, -35.1980 -5.8815))', 4326),
     'AVAILABLE', 'b0000000-0000-4000-8000-000000000001'),
    ('a0000000-0000-4000-8000-000000000003',
     'Capim Macio residential lot',
     'Quiet street close to schools and shopping. Water and power at the lot line.',
     390000.00, 'Carla Mendes', '(84) 90000-0003', 'carla.mendes@example.com',
     ST_GeomFromText('POLYGON((-35.2075 -5.8640, -35.2060 -5.8640, -35.2060 -5.8628, -35.2075 -5.8628, -35.2075 -5.8640))', 4326),
     'AVAILABLE', 'b0000000-0000-4000-8000-000000000001'),
    ('a0000000-0000-4000-8000-000000000004',
     'Lagoa Nova irregular lot',
     'Five sided lot near the city center, suited for a small commercial building.',
     620000.00, 'Diego Rocha', '(84) 90000-0004', NULL,
     ST_GeomFromText('POLYGON((-35.2140 -5.8195, -35.2122 -5.8195, -35.2118 -5.8182, -35.2130 -5.8172, -35.2142 -5.8182, -35.2140 -5.8195))', 4326),
     'AVAILABLE', 'b0000000-0000-4000-8000-000000000001'),
    ('a0000000-0000-4000-8000-000000000005',
     'Redinha lot near the river',
     'Large lot on the north bank of the Potengi, with a view of the bridge.',
     350000.00, 'Elisa Prado', NULL, 'elisa.prado@example.com',
     ST_GeomFromText('POLYGON((-35.2085 -5.7480, -35.2060 -5.7480, -35.2060 -5.7460, -35.2085 -5.7460, -35.2085 -5.7480))', 4326),
     'SOLD', 'b0000000-0000-4000-8000-000000000001'),
    ('a0000000-0000-4000-8000-000000000006',
     'Pitimbu green area lot',
     'Wooded lot on the south side of the city, close to the Pitimbu river park.',
     540000.00, 'Felipe Araujo', '(84) 90000-0006', 'felipe.araujo@example.com',
     ST_GeomFromText('POLYGON((-35.2290 -5.8680, -35.2260 -5.8680, -35.2260 -5.8655, -35.2290 -5.8655, -35.2290 -5.8680))', 4326),
     'AVAILABLE', 'b0000000-0000-4000-8000-000000000001')
-- Only the owner is updated: databases seeded before accounts existed have these lots without one.
-- Other columns are left alone, so edits made through the API survive a new run of the seed
ON CONFLICT (id) DO UPDATE SET owner_id = EXCLUDED.owner_id;
