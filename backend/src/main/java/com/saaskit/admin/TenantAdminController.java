package com.saaskit.admin;

import com.saaskit.audit.Audited;
import com.saaskit.tenancy.Tenant;
import com.saaskit.tenancy.TenantProvisioningService;
import com.saaskit.tenancy.TenantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tenant management. ADMIN role only; these endpoints operate on the
 * public schema (the tenant registry), never inside a tenant.
 */
@RestController
@RequestMapping("/api/admin/tenants")
@PreAuthorize("hasRole('ADMIN')")
public class TenantAdminController {

    private final TenantRepository tenants;
    private final TenantProvisioningService provisioning;

    public TenantAdminController(TenantRepository tenants, TenantProvisioningService provisioning) {
        this.tenants = tenants;
        this.provisioning = provisioning;
    }

    public record CreateTenantRequest(
            @NotBlank String name,
            @NotBlank @Pattern(regexp = "[a-z0-9_]+", message = "slug must be lowercase alphanumeric") String slug,
            String plan) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "TENANT_PROVISIONED", entity = "tenant")
    public Tenant provision(@Valid @RequestBody CreateTenantRequest req) {
        return provisioning.provision(req.name(), req.slug(), req.plan());
    }

    @GetMapping
    public List<Tenant> list() {
        return tenants.findAll();
    }

    @GetMapping("/{id}")
    public Tenant get(@PathVariable UUID id) {
        return tenants.findById(id).orElseThrow(() -> new IllegalArgumentException("unknown tenant"));
    }
}
