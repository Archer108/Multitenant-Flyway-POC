package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Extract tenant dynamically (e.g., from a header or parameter)
        String tenant = request.getHeader("X-TenantID"); // or request.getParameter("tenant")
        if (tenant != null && !tenant.isBlank()) {
            TenantContextHolder.setCurrentTenant(tenant);
        } else {
            // Optionally, log or handle missing tenant case here
            log.warn("Tenant not provided in request; using default datasource.");
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
