package com.saaskit.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * One row per Stripe event we have processed. Stripe retries deliveries,
 * so the webhook handler checks this table first and skips duplicates.
 */
@Entity
@Table(name = "stripe_events", schema = "public")
public class StripeEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt = Instant.now();

    protected StripeEvent() {
    }

    public StripeEvent(String eventId, String eventType, String tenantId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.tenantId = tenantId;
    }

    public String getEventId() {
        return eventId;
    }
}
