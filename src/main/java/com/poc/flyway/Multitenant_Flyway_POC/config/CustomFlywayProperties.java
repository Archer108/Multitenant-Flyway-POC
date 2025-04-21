package com.poc.flyway.Multitenant_Flyway_POC.config;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "custom.flyway")
public class CustomFlywayProperties {

    private FlywaySettings primary;
    private FlywaySettings config;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomFlywayProperties that = (CustomFlywayProperties) o;
        return Objects.equals(primary, that.primary) && Objects.equals(config, that.config);
    }

    @Override
    public int hashCode() {
        return Objects.hash(primary, config);
    }

    @Override
    public String toString() {
        return "CustomFlywayProperties{" + "primary=" + primary + ", config=" + config + '}';
    }

    @Getter
    @Setter
    public static class FlywaySettings {

        private String baselineVersion;
        private boolean baselineOnMigrate;
        private boolean outOfOrder;
        private String locations;
        private String table;
        private String url;
        private String user;
        private String password;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FlywaySettings that = (FlywaySettings) o;
            return baselineOnMigrate == that.baselineOnMigrate
                    && outOfOrder == that.outOfOrder
                    && Objects.equals(baselineVersion, that.baselineVersion)
                    && Objects.equals(locations, that.locations)
                    && Objects.equals(table, that.table)
                    && Objects.equals(url, that.url)
                    && Objects.equals(user, that.user)
                    && Objects.equals(password, that.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    baselineVersion,
                    baselineOnMigrate,
                    outOfOrder,
                    locations,
                    table,
                    url,
                    user,
                    password);
        }

        @Override
        public String toString() {
            return "FlywaySettings{"
                    + "baselineVersion='"
                    + baselineVersion
                    + '\''
                    + ", baselineOnMigrate="
                    + baselineOnMigrate
                    + ", outOfOrder="
                    + outOfOrder
                    + ", locations='"
                    + locations
                    + '\''
                    + ", table='"
                    + table
                    + '\''
                    + ", url='"
                    + url
                    + '\''
                    + ", user='"
                    + user
                    + '\''
                    + ", password='"
                    + password
                    + '\''
                    + '}';
        }
    }
}
