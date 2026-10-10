package com.tenantkit.billing;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tenantkit.tenancy.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Receives Stripe events. Every request is authenticated by Stripe's
 * signature header, not by JWT: this endpoint is server-to-server and
 * intentionally lives under /api/public. The tenant comes from the
 * metadata we attached at checkout time, never from the caller.
 */
@RestController
@RequestMapping("/api/public/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final SubscriptionService subscriptions;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    public StripeWebhookController(SubscriptionService subscriptions) {
        this.subscriptions = subscriptions;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String sigHeader) {
        if (webhookSecret.isBlank()) {
            log.warn("stripe webhook received but no webhook secret configured; ignoring");
            return ResponseEntity.ok("ignored");
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        try {
            switch (event.getType()) {
                case "checkout.session.completed" -> onCheckoutCompleted(event);
                case "customer.subscription.updated", "customer.subscription.created" ->
                        onSubscriptionChanged(event);
                case "customer.subscription.deleted" -> onSubscriptionDeleted(event);
                default -> log.debug("ignoring stripe event {}", event.getType());
            }
        } finally {
            TenantContext.clear();
        }
        return ResponseEntity.ok("ok");
    }

    private String tenantOf(Event event) {
        return switch (event.getType()) {
            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                yield session.getMetadata().get("tenant_id");
            }
            default -> {
                Subscription sub = (Subscription) event.getDataObjectDeserializer()
                        .getObject().orElseThrow();
                yield sub.getMetadata().get("tenant_id");
            }
        };
    }

    @Transactional
    public void onCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
        String tenantId = tenantOf(event);
        if (tenantId == null) {
            log.warn("checkout session without tenant_id metadata: {}", session.getId());
            return;
        }
        TenantContext.setTenantId(tenantId);
        subscriptions.activate(session.getSubscription(),
                session.getMetadata().getOrDefault("plan", "PRO"));
        log.info("subscription activated for tenant {}", tenantId);
    }

    @Transactional
    public void onSubscriptionChanged(Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        String tenantId = tenantOf(event);
        if (tenantId == null) {
            return;
        }
        TenantContext.setTenantId(tenantId);
        subscriptions.sync(sub.getId(), sub.getStatus(),
                sub.getCurrentPeriodEnd());
    }

    @Transactional
    public void onSubscriptionDeleted(Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        String tenantId = tenantOf(event);
        if (tenantId == null) {
            return;
        }
        TenantContext.setTenantId(tenantId);
        subscriptions.sync(sub.getId(), "canceled", null);
        log.info("subscription canceled for tenant {}", tenantId);
    }
}
