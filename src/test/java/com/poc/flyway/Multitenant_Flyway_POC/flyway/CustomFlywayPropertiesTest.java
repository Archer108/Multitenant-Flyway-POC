package com.poc.flyway.Multitenant_Flyway_POC.flyway;

import static org.junit.jupiter.api.Assertions.*;

import com.poc.flyway.Multitenant_Flyway_POC.config.CustomFlywayProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CustomFlywayPropertiesTest {

    private CustomFlywayProperties customFlywayProperties;
    private CustomFlywayProperties.FlywaySettings primarySettings;
    private CustomFlywayProperties.FlywaySettings configSettings;

    private CustomFlywayProperties sameCustomFlywayProperties;
    private CustomFlywayProperties differentCustomFlywayProperties;

    @BeforeEach
    public void setUp() {
        customFlywayProperties = new CustomFlywayProperties();

        primarySettings = new CustomFlywayProperties.FlywaySettings();
        primarySettings.setBaselineVersion("1");
        primarySettings.setBaselineOnMigrate(true);
        primarySettings.setOutOfOrder(false);
        primarySettings.setLocations("classpath:db/migration/primary");
        primarySettings.setTable("primary_schema_history");
        primarySettings.setUrl("jdbc:dummy:primarydb");
        primarySettings.setUser("primary_user");
        primarySettings.setPassword("primary_pass");

        configSettings = new CustomFlywayProperties.FlywaySettings();
        configSettings.setBaselineVersion("0");
        configSettings.setBaselineOnMigrate(false);
        configSettings.setOutOfOrder(true);
        configSettings.setLocations("classpath:db/migration/config");
        configSettings.setTable("config_schema_history");
        configSettings.setUrl("jdbc:dummy:configdb");
        configSettings.setUser("config_user");
        configSettings.setPassword("config_pass");

        customFlywayProperties.setPrimary(primarySettings);
        customFlywayProperties.setConfig(configSettings);

        sameCustomFlywayProperties = new CustomFlywayProperties();
        sameCustomFlywayProperties.setPrimary(primarySettings);
        sameCustomFlywayProperties.setConfig(configSettings);

        differentCustomFlywayProperties = new CustomFlywayProperties();
        CustomFlywayProperties.FlywaySettings differentSettings =
                new CustomFlywayProperties.FlywaySettings();
        differentSettings.setBaselineVersion("2");
        differentCustomFlywayProperties.setPrimary(differentSettings);
        differentCustomFlywayProperties.setConfig(differentSettings);
    }

    @Test
    public void testPrimaryFlywaySettings() {
        assertNotNull(customFlywayProperties.getPrimary());
        assertEquals("1", primarySettings.getBaselineVersion());
        assertTrue(primarySettings.isBaselineOnMigrate());
        assertFalse(primarySettings.isOutOfOrder());
        assertEquals("classpath:db/migration/primary", primarySettings.getLocations());
        assertEquals("primary_schema_history", primarySettings.getTable());
        assertEquals("jdbc:dummy:primarydb", primarySettings.getUrl());
        assertEquals("primary_user", primarySettings.getUser());
        assertEquals("primary_pass", primarySettings.getPassword());
    }

    @Test
    public void testConfigFlywaySettings() {
        assertNotNull(customFlywayProperties.getConfig());
        assertEquals("0", configSettings.getBaselineVersion());
        assertFalse(configSettings.isBaselineOnMigrate());
        assertTrue(configSettings.isOutOfOrder());
        assertEquals("classpath:db/migration/config", configSettings.getLocations());
        assertEquals("config_schema_history", configSettings.getTable());
        assertEquals("jdbc:dummy:configdb", configSettings.getUrl());
        assertEquals("config_user", configSettings.getUser());
        assertEquals("config_pass", configSettings.getPassword());
    }

    @Test
    public void testEqualsForFlywaySettings() {
        assertTrue(primarySettings.equals(primarySettings)); // Self-comparison
        assertTrue(
                primarySettings.equals(customFlywayProperties.getPrimary())); // Identical objects
        assertFalse(primarySettings.equals(configSettings)); // Different objects
        assertFalse(primarySettings.equals(null)); // Null comparison
        assertFalse(primarySettings.equals("NotAFlywaySettings")); // Different class

        // Test with null fields
        CustomFlywayProperties.FlywaySettings nullSettings =
                new CustomFlywayProperties.FlywaySettings();
        assertNotEquals(primarySettings, nullSettings);

        nullSettings.setBaselineVersion(null);
        assertNotEquals(primarySettings, nullSettings);

        nullSettings.setBaselineOnMigrate(true);
        nullSettings.setOutOfOrder(false);
        nullSettings.setLocations(null);
        nullSettings.setTable(null);
        nullSettings.setUrl(null);
        nullSettings.setUser(null);
        nullSettings.setPassword(null);
        assertNotEquals(primarySettings, nullSettings);
    }

    @Test
    public void testHashCodeForFlywaySettings() {
        assertEquals(primarySettings.hashCode(), customFlywayProperties.getPrimary().hashCode());
        assertNotEquals(primarySettings.hashCode(), configSettings.hashCode());

        // Test with null fields
        CustomFlywayProperties.FlywaySettings nullSettings =
                new CustomFlywayProperties.FlywaySettings();
        assertNotEquals(primarySettings.hashCode(), nullSettings.hashCode());
    }

    @Test
    public void testEqualsForCustomFlywayProperties() {
        assertEquals(customFlywayProperties, customFlywayProperties); // Self-comparison
        assertEquals(customFlywayProperties, sameCustomFlywayProperties); // Identical objects
        assertNotEquals(
                customFlywayProperties, differentCustomFlywayProperties); // Different objects
        assertNotEquals(null, customFlywayProperties); // Null comparison
        assertNotEquals("NotAFlywayProperties", customFlywayProperties); // Different class
    }

    @Test
    public void testHashCodeForCustomFlywayProperties() {
        assertEquals(customFlywayProperties.hashCode(), sameCustomFlywayProperties.hashCode());
        assertNotEquals(
                customFlywayProperties.hashCode(), differentCustomFlywayProperties.hashCode());
    }

    @Test
    public void testToStringForFlywaySettings() {
        String expected =
                "FlywaySettings{"
                        + "baselineVersion='1', "
                        + "baselineOnMigrate=true, "
                        + "outOfOrder=false, "
                        + "locations='classpath:db/migration/primary', "
                        + "table='primary_schema_history', "
                        + "url='jdbc:dummy:primarydb', "
                        + "user='primary_user', "
                        + "password='primary_pass'"
                        + "}";
        assertEquals(expected, primarySettings.toString());
    }

    @Test
    public void testToStringForCustomFlywayProperties() {
        String expected =
                "CustomFlywayProperties{"
                        + "primary=FlywaySettings{"
                        + "baselineVersion='1', baselineOnMigrate=true, outOfOrder=false, "
                        + "locations='classpath:db/migration/primary', table='primary_schema_history', "
                        + "url='jdbc:dummy:primarydb', user='primary_user', password='primary_pass'"
                        + "}, config=FlywaySettings{"
                        + "baselineVersion='0', baselineOnMigrate=false, outOfOrder=true, "
                        + "locations='classpath:db/migration/config', table='config_schema_history', "
                        + "url='jdbc:dummy:configdb', user='config_user', password='config_pass'"
                        + "}}";

        assertEquals(expected, customFlywayProperties.toString());
    }

    @Test
    public void testEqualsForFlywaySettingsWithNullAndNonNullFields() {
        // Initialize an object with some null and non-null values
        CustomFlywayProperties.FlywaySettings testSettings =
                new CustomFlywayProperties.FlywaySettings();

        // Matching primarySettings except for null in the 'locations' field
        testSettings.setBaselineVersion("1");
        testSettings.setBaselineOnMigrate(true);
        testSettings.setOutOfOrder(false);
        testSettings.setLocations(null); // This is the key difference
        testSettings.setTable("primary_schema_history");
        testSettings.setUrl("jdbc:dummy:primarydb");
        testSettings.setUser("primary_user");
        testSettings.setPassword("primary_pass");

        // Update primarySettings to have a null 'locations' field
        primarySettings.setLocations(null);

        // They should be equal since only the 'locations' field is null in both
        assertEquals(primarySettings, testSettings);

        // Test with more null fields, setting fields to null one by one
        testSettings.setTable(null);
        primarySettings.setTable(null);
        assertEquals(primarySettings, testSettings);

        testSettings.setUrl(null);
        primarySettings.setUrl(null);
        assertEquals(primarySettings, testSettings);

        testSettings.setUser(null);
        primarySettings.setUser(null);
        assertEquals(primarySettings, testSettings);

        testSettings.setPassword(null);
        primarySettings.setPassword(null);
        assertEquals(primarySettings, testSettings);

        // Test when baselineVersion is also null
        testSettings.setBaselineVersion(null);
        primarySettings.setBaselineVersion(null);
        assertEquals(primarySettings, testSettings);
    }

    @Test
    public void testEqualsWithEmptyAndDifferentStrings() {
        CustomFlywayProperties.FlywaySettings emptyStringSettings =
                new CustomFlywayProperties.FlywaySettings();

        // Test empty vs non-empty strings for all fields
        emptyStringSettings.setBaselineVersion("");
        emptyStringSettings.setLocations("");
        emptyStringSettings.setTable("");
        emptyStringSettings.setUrl("");
        emptyStringSettings.setUser("");
        emptyStringSettings.setPassword("");

        assertNotEquals(primarySettings, emptyStringSettings);

        // Test different strings for all fields
        CustomFlywayProperties.FlywaySettings differentStringSettings =
                new CustomFlywayProperties.FlywaySettings();
        differentStringSettings.setBaselineVersion("2");
        differentStringSettings.setLocations("differentLocation");
        differentStringSettings.setTable("differentTable");
        differentStringSettings.setUrl("jdbc:dummy:differentdb");
        differentStringSettings.setUser("different_user");
        differentStringSettings.setPassword("different_pass");

        assertNotEquals(primarySettings, differentStringSettings);
    }
}
