-- Tenant registry. The only table that lives in the public schema.
CREATE TABLE tenants (
    id                UUID PRIMARY KEY,
    name              TEXT NOT NULL,
    slug              TEXT NOT NULL UNIQUE,
    schema_name       TEXT NOT NULL UNIQUE,
    plan              TEXT NOT NULL DEFAULT 'FREE',
    status            TEXT NOT NULL DEFAULT 'ACTIVE',
    stripe_customer_id TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
