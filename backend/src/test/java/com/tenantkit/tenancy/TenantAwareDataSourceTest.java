package com.tenantkit.tenancy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantAwareDataSourceTest {

    @Test
    void schemaNamePrefixesWithTenant() {
        assertEquals("tenant_acme", TenantAwareDataSource.schemaName("acme"));
    }

    @Test
    void schemaNameLowercases() {
        assertEquals("tenant_acme", TenantAwareDataSource.schemaName("Acme"));
    }

    @Test
    void schemaNameSanitizesUnsafeChars() {
        assertEquals("tenant_acme_corp", TenantAwareDataSource.schemaName("acme-corp"));
        assertEquals("tenant_acme_corp", TenantAwareDataSource.schemaName("acme corp"));
        assertEquals("tenant_acme_corp", TenantAwareDataSource.schemaName("acme.corp"));
    }

    @Test
    void schemaNameKeepsUnderscoresAndDigits() {
        assertEquals("tenant_acme_2", TenantAwareDataSource.schemaName("acme_2"));
    }

    @Test
    void differentTenantsGetDifferentSchemas() {
        assertNotEquals(
                TenantAwareDataSource.schemaName("alpha"),
                TenantAwareDataSource.schemaName("beta"));
    }
}
