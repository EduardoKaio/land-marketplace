-- Idempotent: V1 already enables it, repeated here so this file is self-contained.
CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE land_lot (
    id            uuid PRIMARY KEY,
    title         varchar(120)   NOT NULL,
    description   text           NOT NULL,
    price         numeric(15, 2) NOT NULL CHECK (price > 0),
    contact_name  varchar(120)   NOT NULL,
    contact_phone varchar(30),
    contact_email varchar(254),
    -- Planar lon/lat polygon. The CHECK rejects self-intersections and other invalid shapes,
    -- which would make ST_Area and the overlap predicates return wrong results.
    geom          geometry(Polygon, 4326) NOT NULL CHECK (ST_IsValid(geom)),
    -- Geodesic area in square meters, computed by PostGIS on the WGS84 spheroid.
    -- Allowed as a generated column because ST_Area(geography) and the cast are IMMUTABLE.
    area_m2       double precision GENERATED ALWAYS AS (ST_Area(geom::geography)) STORED,
    status        varchar(20)    NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE', 'SOLD')),
    created_at    timestamptz    NOT NULL DEFAULT now(),
    updated_at    timestamptz    NOT NULL DEFAULT now(),
    CONSTRAINT land_lot_contact_channel_chk CHECK (contact_phone IS NOT NULL OR contact_email IS NOT NULL)
);

-- Planar bounding box index on geometry. Serves &&, ST_Intersects and ST_Overlaps,
-- used by the overlap check on create and by the map viewport (bbox) listing.
CREATE INDEX land_lot_geom_gist ON land_lot USING GIST (geom);

-- Expression index on geography. Serves distance queries in meters such as
-- ST_DWithin(geom::geography, center, radius_m) used by the circle search.
-- The planner only uses it when the query repeats the exact expression geom::geography.
CREATE INDEX land_lot_geog_gist ON land_lot USING GIST ((geom::geography));
