/*
package com.adp.benefits.carrier.config.app;


@Configuration
public class EnrollmentCacheConfig {

    @Bean
    public Cache<MultiTenantPageKey, PageCursorBook> enrollmentCursorCache(
            @Value("${cpm.enrollment.cache.maxSize:200}") long maxSize,
            @Value("${cpm.enrollment.cache.ttl-minutes:10}") long ttlMinutes) {

        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterAccess(Duration.ofMinutes(ttlMinutes))
                .recordStats()
                .build();
    }
}
*/
