package com.saaskit.tenancy;

/**
 * Holds the current tenant for the executing thread. The tenant-aware
 * DataSource reads this on every connection checkout and points the
 * connection at the tenant's schema, so repositories never think about
 * tenancy at all.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
