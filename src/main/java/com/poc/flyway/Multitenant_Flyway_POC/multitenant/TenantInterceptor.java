package com.poc.flyway.Multitenant_Flyway_POC.multitenant;/*
package com.adp.benefits.carrier.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String tenantId = request.getHeader("X-Tenant-ID");

        if (tenantId == null || tenantId.isEmpty()) {
            log.warn("🚨 No Tenant ID provided in headers! Defaulting to primary_local.");
            TenantContextHolder.setCurrentTenant("primary_local");
        } else {
            log.info("📌 Setting tenant context for: {}", tenantId);
            TenantContextHolder.setCurrentTenant(tenantId);
        }

        log.info("📌 Tenant Context after setting: {}", TenantContextHolder.getCurrentTenant());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContextHolder.clear(); // Clear tenant context after request processing
    }
}*/
