package com.poc.flyway.Multitenant_Flyway_POC.appConfig;

/**
 * Maintains tenant context in a thread-safe manner for each request.
 */
public class TenantContextHolder {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
        // Utility class - prevent instantiation
    }

    /**
     * Sets the current tenant ID in thread-local context.
     * @param tenantId the tenant identifier
     */
    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Retrieves the current tenant ID from thread-local context.
     * @return the current tenant ID or null
     */
    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    /**
     * Clears the current thread-local context.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}