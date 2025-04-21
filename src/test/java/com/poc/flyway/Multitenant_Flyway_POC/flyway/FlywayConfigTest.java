package com.poc.flyway.Multitenant_Flyway_POC.flyway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.poc.flyway.Multitenant_Flyway_POC.config.CustomFlywayProperties;
import com.poc.flyway.Multitenant_Flyway_POC.config.FlywayConfig;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FlywayConfigTest {

    @InjectMocks private FlywayConfig flywayConfig;

    @Mock private CustomFlywayProperties customFlywayProperties;

    @Mock private CustomFlywayProperties.FlywaySettings primarySettings;

    @Mock private CustomFlywayProperties.FlywaySettings configSettings;

    @Mock private DataSource primaryDataSource;

    @Mock private DataSource configDataSource;

    @Mock private Flyway flyway;

    @Mock private MigrateResult migrateResult;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        when(customFlywayProperties.getPrimary()).thenReturn(primarySettings);
        when(customFlywayProperties.getConfig()).thenReturn(configSettings);

        when(primarySettings.getUrl()).thenReturn("jdbc:postgresql://localhost:5432/dummydb");
        when(primarySettings.getUser()).thenReturn("dummy_user");
        when(primarySettings.getPassword()).thenReturn("dummy_pass");
        when(primarySettings.isBaselineOnMigrate()).thenReturn(true);
        when(primarySettings.getBaselineVersion()).thenReturn("0");
        when(primarySettings.isOutOfOrder()).thenReturn(true);
        when(primarySettings.getLocations()).thenReturn("classpath:db/migration/primary");
        when(primarySettings.getTable()).thenReturn("primary_schema_history");

        when(configSettings.getUrl()).thenReturn("jdbc:postgresql://localhost:5432/dummydb");
        when(configSettings.getUser()).thenReturn("dummy_user");
        when(configSettings.getPassword()).thenReturn("dummy_pass");
        when(configSettings.isBaselineOnMigrate()).thenReturn(true);
        when(configSettings.getBaselineVersion()).thenReturn("0");
        when(configSettings.isOutOfOrder()).thenReturn(true);
        when(configSettings.getLocations()).thenReturn("classpath:db/migration/config");
        when(configSettings.getTable()).thenReturn("config_schema_history");

        // Mock the Flyway migration method to return a mock MigrateResult
        when(flyway.migrate()).thenReturn(migrateResult);
    }

    @Test
    public void testPrimaryFlyway() {
        Flyway flyway = spy(flywayConfig.primaryFlyway(primaryDataSource));
        assertNotNull(flyway);
        verify(flyway, never()).migrate(); // Verifies that migrate() is not called during the test
    }

    @Test
    public void testConfigFlyway() {
        Flyway flyway = spy(flywayConfig.configFlyway(configDataSource));
        assertNotNull(flyway);
        verify(flyway, never()).migrate(); // Verifies that migrate() is not called during the test
    }
}
