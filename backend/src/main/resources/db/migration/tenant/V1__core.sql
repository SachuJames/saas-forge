-- Core tenant schema: users, audit trail, and billing state.
-- This migration runs once per tenant, inside that tenant's schema.

CREATE TABLE app_users (
    id            UUID PRIMARY KEY,
    keycloak_sub  TEXT UNIQUE,
    email         TEXT NOT NULL,
    name          TEXT NOT NULL,
    role          TEXT NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id          UUID PRIMARY KEY,
    actor       TEXT NOT NULL,
    action      TEXT NOT NULL,
    entity      TEXT,
    entity_id   TEXT,
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);

CREATE TABLE subscriptions (
    id                     UUID PRIMARY KEY,
    stripe_subscription_id TEXT UNIQUE,
    plan                   TEXT NOT NULL DEFAULT 'FREE',
    status                 TEXT NOT NULL DEFAULT 'active',
    current_period_end     TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
