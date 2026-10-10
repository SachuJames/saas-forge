package com.saaskit.tenancy;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Points every checked-out connection at the current tenant's schema.
 * Because the schema is set on the connection itself, Hibernate and Flyway
 * need no multi-tenancy configuration: they just use whatever schema the
 * connection is already on. Every getConnection call re-applies the schema,
 * so a pooled connection can never leak one tenant's schema into another.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    @Override
    public Connection getConnection() throws SQLException {
        return applySchema(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return applySchema(super.getConnection(username, password));
    }

    private Connection applySchema(Connection connection) throws SQLException {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null && !tenantId.isBlank()) {
            connection.setSchema(schemaName(tenantId));
        }
        return connection;
    }

    /** Maps a tenant id to its Postgres schema: tenant ids become tenant_<slug>. */
    public static String schemaName(String tenantId) {
        String slug = tenantId.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return "tenant_" + slug;
    }
}
