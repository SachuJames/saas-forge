package com.saaskit.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * A pooled connection must never leak one tenant's schema into another
 * request. With a tenant in context the connection points at the tenant
 * schema; without one it is reset to the shared public schema.
 */
class TenantAwareDataSourceSchemaResetTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void setsTenantSchemaWhenTenantPresent() throws Exception {
        TenantContext.setTenantId("acme");
        var ds = new TenantAwareDataSource();
        var delegate = mock(javax.sql.DataSource.class);
        var conn = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(conn);
        ds.setTargetDataSource(delegate);

        ds.getConnection();

        verify(conn).setSchema("tenant_acme");
        verify(conn, never()).setSchema("public");
    }

    @Test
    void resetsToPublicSchemaWhenNoTenant() throws Exception {
        var ds = new TenantAwareDataSource();
        var delegate = mock(javax.sql.DataSource.class);
        var conn = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(conn);
        ds.setTargetDataSource(delegate);

        ds.getConnection();

        verify(conn).setSchema("public");
    }
}
