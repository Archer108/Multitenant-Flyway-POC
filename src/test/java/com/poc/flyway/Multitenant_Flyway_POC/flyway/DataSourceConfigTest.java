package com.poc.flyway.Multitenant_Flyway_POC.flyway;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.poc.flyway.Multitenant_Flyway_POC.config.DataSourceConfig;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;

public class DataSourceConfigTest {

    @InjectMocks private DataSourceConfig dataSourceConfig;

    @Mock private DataSourceProperties primaryDataSourceProperties;

    @Mock private DataSourceProperties configDataSourceProperties;

    @Mock private DataSource primaryDataSource;

    @Mock private DataSource configDataSource;

    @SuppressWarnings("rawtypes")
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Use raw type for DataSourceBuilder to avoid generic issues
        DataSourceBuilder primaryDataSourceBuilder = mock(DataSourceBuilder.class);
        DataSourceBuilder configDataSourceBuilder = mock(DataSourceBuilder.class);

        // Mock the behavior of DataSourceProperties to return the mocked DataSourceBuilder
        when(primaryDataSourceProperties.initializeDataSourceBuilder())
                .thenReturn(primaryDataSourceBuilder);
        when(configDataSourceProperties.initializeDataSourceBuilder())
                .thenReturn(configDataSourceBuilder);

        // Mock the DataSourceBuilder to return the mocked DataSource
        when(primaryDataSourceBuilder.build()).thenReturn(primaryDataSource);
        when(configDataSourceBuilder.build()).thenReturn(configDataSource);
    }

    @Test
    public void testPrimaryDataSource() {
        DataSource dataSource = dataSourceConfig.primaryDataSource(primaryDataSourceProperties);
        assertNotNull(dataSource);
        verify(primaryDataSourceProperties, times(1)).initializeDataSourceBuilder();
    }

    @Test
    public void testConfigDataSource() {
        DataSource dataSource = dataSourceConfig.configDataSource(configDataSourceProperties);
        assertNotNull(dataSource);
        verify(configDataSourceProperties, times(1)).initializeDataSourceBuilder();
    }
}
