package com.saaskit.billing;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.saaskit.tenancy.TenantContext;
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
 *
 * Two safety properties: the handler fails closed when the webhook secret
 * is not configured (Stripe retries on non-2xx, so the event is not lost),
 * and every event id is recorded in the public stripe_events table so
 * retried or duplicate deliveries are processed exactly once.
 */
@RestController
@RequestMapping("/api/public/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final SubscriptionService subscriptions;
    private final StripeEventRepository events;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    public StripeWebhookController(SubscriptionService subscriptions,
                                   StripeEventRepository events) {
        this.subscriptions = subscriptions;
        this.events = events;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader("Stripe-Signature") String sigHeader) {
        if (webhookSecret.isBlank()) {
            // Fail closed: a 5xx makes Stripe retry instead of silently
            // dropping a billing event we never processed.
            log.error("stripe webhook received but APP_STRIPE_WEBHOOK_SECRET is not configured");
            return ResponseEntity.internalServerError().body("webhook not configured");
        }
        final Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            log.warn("stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        try {
            if (events.existsById(event.getId())) {
                log.info("skipping duplicate stripe event {}", event.getId());
                return ResponseEntity.ok("duplicate");
            }
            String tenantId = tenantOf(event);
            events.save(new StripeEvent(event.getId(), event.getType(), tenantId));

            switch (event.getType()) {
                case "checkout.session.completed" -> onCheckoutCompleted(event, tenantId);
                case "customer.subscription.updated", "customer.subscription.created" ->
                        onSubscriptionChanged(event, tenantId);
                case "customer.subscription.deleted" -> onSubscriptionDeleted(event, tenantId);
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
    public void onCheckoutCompleted(Event event, String tenantId) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElseThrow();
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
    public void onSubscriptionChanged(Event event, String tenantId) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        if (tenantId == null) {
            log.warn("subscription event without tenant_id metadata: {}", sub.getId());
            return;
        }
        TenantContext.setTenantId(tenantId);
        Long periodEnd = sub.getItems().getData().stream()
                .map(item -> item.getCurrentPeriodEnd())
                .filter(p -> p != null)
                .findFirst().orElse(null);
        subscriptions.sync(sub.getId(), sub.getStatus(), periodEnd);
    }

    @Transactional
    public void onSubscriptionDeleted(Event event, String tenantId) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer().getObject().orElseThrow();
        if (tenantId == null) {
            log.warn("subscription deleted event without tenant_id metadata: {}", sub.getId());
            return;
        }
        TenantContext.setTenantId(tenantId);
        subscriptions.sync(sub.getId(), "canceled", null);
        log.info("subscription canceled for tenant {}", tenantId);
    }
}
