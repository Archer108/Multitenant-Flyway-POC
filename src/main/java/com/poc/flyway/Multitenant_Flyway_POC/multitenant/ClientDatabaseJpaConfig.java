package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.adp.benefits.carrier.entity.client",
        entityManagerFactoryRef = "clientEntityManagerFactory",
        transactionManagerRef = "clientTransactionManager")
public class ClientDatabaseJpaConfig {

    @Value("${spring.jpa.database-platform}")
    private String databasePlatform;

    private final MultiTenantDataSource multiTenantDataSource;

    public ClientDatabaseJpaConfig(
            @Qualifier("multiTenantDataSource") MultiTenantDataSource multiTenantDataSource) {
        this.multiTenantDataSource = multiTenantDataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean clientEntityManagerFactory() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(multiTenantDataSource);
        em.setPackagesToScan("com.adp.benefits.carrier.entity.client");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", databasePlatform);
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public JpaTransactionManager clientTransactionManager(
            @Qualifier("clientEntityManagerFactory")
                    LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}
