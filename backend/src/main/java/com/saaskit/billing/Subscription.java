package com.saaskit.billing;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * The tenant's billing state, mirrored from Stripe. Stripe is the source
 * of truth for money; this row is a local cache that makes "is this tenant
 * allowed the PRO features?" a single indexed lookup instead of an API
 * call. Webhooks keep it in sync.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
public class Subscription {

    @Id
    private UUID id;

    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;

    @Column(nullable = false)
    private String plan = "FREE";

    @Column(nullable = false)
    private String status = "active";

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    /** True when the tenant may use paid features right now. */
    public boolean isUsable() {
        return status.equals("active") || status.equals("trialing");
    }
}
