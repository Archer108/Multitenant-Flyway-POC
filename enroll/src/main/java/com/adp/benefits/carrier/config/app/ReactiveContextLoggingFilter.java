package com.adp.benefits.carrier.config.app;

import com.adp.benefits.carrier.config.database.TenantContextHolder;
import com.adp.benefits.carrier.config.database.TenantDatabaseLoader;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class ReactiveContextLoggingFilter implements WebFilter {

    private static final String TRACE_ID = "traceId";
    private static final String SPAN_ID = "spanId";
    private static final String TENANT_ID = "tenantId";
    private static final String HTTP_METHOD = "method";
    private static final String REQUEST_URI = "uri";
    private static final String STATUS_CODE = "status";
    private static final String DURATION_MS = "durationMs";

    private static final String HEADER_TRACE_ID = "X-TraceId";
    private static final String HEADER_SPAN_ID = "X-SpanId";
    private static final String UNKNOWN_TENANT = "UNKNOWN";
    private static final String TENANT_HEADER = "X-OID";

    private static final Set<String> EXCLUDED_PATHS = Set.of("/actuator/", "/favicon.ico");

    private final TenantDatabaseLoader tenantDatabaseLoader;

    @Override
    public @NotNull Mono<Void> filter(ServerWebExchange exchange, @NotNull WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String uri = request.getURI().toString();

        // Skip logging for actuator endpoints and favicon requests
        Mono<Void> bypassResult = handleExcludedPaths(exchange, chain, uri, request);
        if (bypassResult != null) return bypassResult;

        long startTime = System.nanoTime();
        String traceId = getOrCreateTraceId(request);
        String spanId = extractSpanId(traceId);
        String method = request.getMethod().name();

        initMdc(traceId, spanId, method, uri);

        String tenant = resolveTenant(request);
        MDC.put(TENANT_ID, tenant);

        writeTraceHeaders(exchange, traceId, spanId);

        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return chain.filter(exchange)
                .contextWrite(context -> context.put("MDC", contextMap))
                .doFinally(
                        signalType -> logCompletion(contextMap, startTime, method, uri, exchange));
    }

    private Mono<Void> handleExcludedPaths(
            ServerWebExchange exchange,
            WebFilterChain chain,
            String uri,
            ServerHttpRequest request) {
        boolean isExcludedPath = EXCLUDED_PATHS.stream().anyMatch(uri::contains);
        if (isExcludedPath) {
            if (log.isTraceEnabled()) {
                String traceId = getOrCreateTraceId(request);
                log.trace(
                        "Excluded path request: {} {} (traceId={})",
                        request.getMethod().name(),
                        uri,
                        traceId);
            }
            return chain.filter(exchange);
        }
        return null;
    }

    private void logCompletion(
            Map<String, String> contextMap,
            long startTime,
            String method,
            String uri,
            ServerWebExchange exchange) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        int status =
                exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value()
                        : 500;
        try {
            MDC.setContextMap(contextMap);
            MDC.put(STATUS_CODE, String.valueOf(status));
            MDC.put(DURATION_MS, String.valueOf(durationMs));
            log.info("{} {} -> {} ({} ms)", method, uri, status, durationMs);
        } finally {
            TenantContextHolder.clear();
            MDC.clear();
        }
    }

    private void writeTraceHeaders(ServerWebExchange exchange, String traceId, String spanId) {
        exchange.getResponse().getHeaders().add(HEADER_TRACE_ID, traceId);
        exchange.getResponse().getHeaders().add(HEADER_SPAN_ID, spanId);
    }

    private void initMdc(String traceId, String spanId, String method, String uri) {
        MDC.clear();
        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
        MDC.put(HTTP_METHOD, method);
        MDC.put(REQUEST_URI, uri);
    }

    private String extractSpanId(String traceId) {
        return traceId.replace("-", "").substring(0, 8);
    }

    private String getOrCreateTraceId(ServerHttpRequest request) {
        String headerTraceId = request.getHeaders().getFirst(HEADER_TRACE_ID);
        return headerTraceId != null && !headerTraceId.isBlank()
                ? headerTraceId
                : UUID.randomUUID().toString();
    }

    private String resolveTenant(ServerHttpRequest request) {
        String oid = request.getHeaders().getFirst(TENANT_HEADER);
        String tenant = UNKNOWN_TENANT;
        if (oid != null && !oid.isBlank()) {
            String tenantId = tenantDatabaseLoader.getTenantIdByOid(oid);
            if (tenantId != null && !tenantId.isBlank()) {
                tenant = tenantId;
                TenantContextHolder.setCurrentTenant(tenant);
            } else {
                log.warn("No tenant found for OID [{}].", oid);
            }
        } else {
            log.warn("Missing or blank X-OID header.");
        }
        log.info("Resolved tenant for OID [{}] -> tenant [{}]", oid, tenant);
        return tenant;
    }
}
