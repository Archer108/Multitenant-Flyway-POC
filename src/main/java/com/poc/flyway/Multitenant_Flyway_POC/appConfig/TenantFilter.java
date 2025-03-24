package com.poc.flyway.Multitenant_Flyway_POC.appConfig;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter extends OncePerRequestFilter {

    private final TenantDatabaseLoader tenantDatabaseLoader;

    public TenantFilter(TenantDatabaseLoader tenantDatabaseLoader) {
        this.tenantDatabaseLoader = tenantDatabaseLoader;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String oid = request.getHeader("X-OID");

        if (oid != null && !oid.isBlank()) {
            String tenantId = tenantDatabaseLoader.getTenantIdByOid(oid);

            if (tenantId != null) {
                TenantContextHolder.setCurrentTenant(tenantId);
                log.debug("📌 Tenant [{}] resolved from OID [{}]", tenantId, oid);
            } else {
                log.warn("⚠ No tenant found for OID [{}]. Using default DataSource.", oid);
            }
        } else {
            log.warn("⚠ Missing or blank X-OID header. Falling back to default DataSource.");
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}