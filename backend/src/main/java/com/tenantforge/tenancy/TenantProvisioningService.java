package com.tenantforge.tenancy;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Creates and prepares a tenant's private schema. Provisioning is three
 * steps, in order: register the tenant in the public schema, create the
 * Postgres schema, and run the tenant Flyway migrations into it. The raw
 * DataSource (not the tenant-aware wrapper) is used for the DDL so the
 * statements run exactly where intended regardless of request context.
 */
@Service
public class TenantProvisioningService {

    private final TenantRepository tenants;
    private final JdbcTemplate jdbc;
    private final DataSource rawDataSource;

    @Value("${app.flyway.tenant-locations:classpath:db/migration/tenant}")
    private String tenantLocations;

    public TenantProvisioningService(TenantRepository tenants, JdbcTemplate jdbc, DataSource rawDataSource) {
        this.tenants = tenants;
        this.jdbc = jdbc;
        this.rawDataSource = rawDataSource;
    }

    @Transactional
    public Tenant provision(String name, String slug, String plan) {
        String cleanSlug = slug.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (tenants.findBySlug(cleanSlug).isPresent()) {
            throw new IllegalArgumentException("tenant slug already exists: " + cleanSlug);
        }
        String schema = TenantAwareDataSource.schemaName(cleanSlug);

        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(cleanSlug);
        tenant.setSchemaName(schema);
        tenant.setPlan(plan == null ? "FREE" : plan);
        tenants.save(tenant);

        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + schema);

        Flyway.configure()
                .dataSource(rawDataSource)
                .schemas(schema)
                .locations(tenantLocations)
                .load()
                .migrate();

        return tenant;
    }
}
