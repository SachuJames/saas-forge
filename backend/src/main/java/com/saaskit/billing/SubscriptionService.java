package com.saaskit.billing;

import com.saaskit.audit.Audited;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Owns the tenant-local subscription mirror. Stripe holds the money;
 * this keeps a local copy so plan checks stay a cheap database lookup.
 */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;

    public SubscriptionService(SubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Transactional(readOnly = true)
    public Subscription current() {
        List<Subscription> all = subscriptions.findAll();
        return all.isEmpty() ? emptySubscription() : all.get(0);
    }

    @Audited(action = "SUBSCRIPTION_ACTIVATED", entity = "subscription")
    @Transactional
    public Subscription activate(String stripeSubscriptionId, String plan) {
        Subscription sub = subscriptions.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseGet(Subscription::new);
        sub.setStripeSubscriptionId(stripeSubscriptionId);
        sub.setPlan(plan);
        sub.setStatus("active");
        return subscriptions.save(sub);
    }

    @Audited(action = "SUBSCRIPTION_SYNCED", entity = "subscription")
    @Transactional
    public Subscription sync(String stripeSubscriptionId, String status, Long periodEndEpoch) {
        Subscription sub = subscriptions.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseGet(Subscription::new);
        sub.setStripeSubscriptionId(stripeSubscriptionId);
        sub.setStatus(status);
        if (periodEndEpoch != null) {
            sub.setCurrentPeriodEnd(Instant.ofEpochSecond(periodEndEpoch));
        }
        return subscriptions.save(sub);
    }

    /** True when the tenant may use paid features right now. */
    @Transactional(readOnly = true)
    public boolean hasUsableSubscription() {
        return current().isUsable() && !"FREE".equals(current().getPlan());
    }

    private Subscription emptySubscription() {
        Subscription sub = new Subscription();
        sub.setPlan("FREE");
        sub.setStatus("active");
        return sub;
    }
}
