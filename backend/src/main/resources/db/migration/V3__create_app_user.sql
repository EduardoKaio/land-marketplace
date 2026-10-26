CREATE TABLE app_user (
    id            uuid PRIMARY KEY,
    name          varchar(120) NOT NULL,
    -- Stored lower case by the application, so a plain UNIQUE makes emails case insensitive
    email         varchar(254) NOT NULL UNIQUE,
    -- BCrypt output is 60 characters
    password_hash varchar(72)  NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now()
);

-- Nullable on purpose: lots created before accounts existed keep working as public listings,
-- and nobody can edit or delete them. Every lot created from now on gets its owner from the
-- access token.
ALTER TABLE land_lot ADD COLUMN owner_id uuid REFERENCES app_user (id);

-- Serves GET /api/me/land-lots
CREATE INDEX land_lot_owner_id_idx ON land_lot (owner_id);
