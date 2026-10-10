package com.saaskit.billing;

import com.stripe.exception.StripeException;
import com.saaskit.audit.Audited;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Billing endpoints for the current tenant.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final StripeService stripe;
    private final SubscriptionService subscriptions;

    public BillingController(StripeService stripe, SubscriptionService subscriptions) {
        this.stripe = stripe;
        this.subscriptions = subscriptions;
    }

    @GetMapping("/subscription")
    public Subscription current() {
        return subscriptions.current();
    }

    public record CheckoutRequest(@NotBlank String plan,
                                  @NotBlank String successUrl,
                                  @NotBlank String cancelUrl) {
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('ADMIN')")
    @Audited(action = "CHECKOUT_STARTED", entity = "subscription")
    public ResponseEntity<Map<String, String>> checkout(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CheckoutRequest req) throws StripeException {
        if (!stripe.isConfigured()) {
            return ResponseEntity.unprocessableEntity()
                    .body(Map.of("error", "billing is not configured"));
        }
        String url = stripe.createCheckoutSession(tenantId, req.plan(), req.successUrl(), req.cancelUrl());
        return ResponseEntity.ok(Map.of("checkout_url", url));
    }

    @GetMapping("/features/pro")
    public Map<String, Boolean> proFeature() {
        // Example of plan gating: this endpoint is only useful on a paid plan.
        return Map.of("allowed", subscriptions.hasUsableSubscription());
    }
}
