package com.tenantkit.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tenantkit.tenancy.Tenant;
import com.tenantkit.tenancy.TenantRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Talks to Stripe. The API key is server-side only and never leaves this
 * service. Every checkout session carries the tenant id in its metadata so
 * the webhook can route the event back to the right tenant.
 */
@Service
public class StripeService {

    private final TenantRepository tenants;

    @Value("${app.stripe.api-key:}")
    private String apiKey;

    @Value("${app.stripe.price-pro:}")
    private String pricePro;

    @Value("${app.stripe.price-enterprise:}")
    private String priceEnterprise;

    public StripeService(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @PostConstruct
    void init() {
        if (!apiKey.isBlank()) {
            Stripe.apiKey = apiKey;
        }
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    /**
     * Creates a Stripe Checkout session for a tenant's plan upgrade.
     * Returns the URL the browser should redirect to.
     */
    public String createCheckoutSession(String tenantSlug, String plan,
                                        String successUrl, String cancelUrl) throws StripeException {
        Tenant tenant = tenants.findBySlug(tenantSlug)
                .orElseThrow(() -> new IllegalArgumentException("unknown tenant: " + tenantSlug));

        String customerId = tenant.getStripeCustomerId();
        if (customerId == null) {
            Customer customer = Customer.create(CustomerCreateParams.builder()
                    .setName(tenant.getName())
                    .putMetadata("tenant_id", tenantSlug)
                    .build());
            customerId = customer.getId();
            tenant.setStripeCustomerId(customerId);
            tenants.save(tenant);
        }

        String priceId = switch (plan.toUpperCase()) {
            case "PRO" -> pricePro;
            case "ENTERPRISE" -> priceEnterprise;
            default -> throw new IllegalArgumentException("unknown plan: " + plan);
        };
        if (priceId.isBlank()) {
            throw new IllegalStateException("no Stripe price configured for plan " + plan);
        }

        Session session = Session.create(SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(customerId)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(priceId)
                        .setQuantity(1L)
                        .build())
                .putMetadata("tenant_id", tenantSlug)
                .putMetadata("plan", plan.toUpperCase())
                .build());
        return session.getUrl();
    }
}
