package com.tenantkit.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void setAndGetTenantId() {
        TenantContext.setTenantId("acme");
        assertEquals("acme", TenantContext.getTenantId());
    }

    @Test
    void clearRemovesTenantId() {
        TenantContext.setTenantId("acme");
        TenantContext.clear();
        assertNull(TenantContext.getTenantId());
    }

    @Test
    void tenantIdIsThreadLocal() throws InterruptedException {
        TenantContext.setTenantId("tenant-a");
        String[] other = new String[1];
        Thread t = new Thread(() -> other[0] = TenantContext.getTenantId());
        t.start();
        t.join();
        assertNull(other[0], "tenant must not leak across threads");
        assertEquals("tenant-a", TenantContext.getTenantId());
    }
}
