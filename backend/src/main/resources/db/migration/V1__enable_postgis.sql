-- The postgis/postgis image already enables the extension, but declaring it here keeps
-- the schema reproducible on any PostgreSQL server that has PostGIS installed.
CREATE EXTENSION IF NOT EXISTS postgis;
