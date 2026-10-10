-- Stripe webhook idempotency. Lives in the public schema because webhooks
-- arrive without a tenant context; the event id is globally unique.
CREATE TABLE stripe_events (
    event_id   TEXT PRIMARY KEY,
    event_type TEXT NOT NULL,
    tenant_id  TEXT,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
