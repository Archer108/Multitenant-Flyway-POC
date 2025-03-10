package com.poc.flyway.Multitenant_Flyway_POC.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "custom.flyway")
public class CustomFlywayProperties {
    private FlywaySettings primary;
    private FlywaySettings config;

    @Data
    public static class FlywaySettings {
        private String baselineVersion;
        private boolean baselineOnMigrate;
        private boolean outOfOrder;
        private String locations;
        private String table;
        private String url;
        private String user;
        private String password;
    }
}