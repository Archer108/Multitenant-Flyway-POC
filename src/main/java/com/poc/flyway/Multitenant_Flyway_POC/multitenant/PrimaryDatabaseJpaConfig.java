package com.poc.flyway.Multitenant_Flyway_POC.multitenant;

import com.adp.benefits.carrier.util.DecryptionUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.adp.benefits.carrier.entity.primary",
        entityManagerFactoryRef = "primaryEntityManagerFactory",
        transactionManagerRef = "primaryTransactionManager")
public class PrimaryDatabaseJpaConfig {

    @Value("${spring.datasource.primary.url}")
    private String url;

    @Value("${spring.datasource.primary.username}")
    private String username;

    @Value("${spring.datasource.primary.password}")
    private String password;

    @Value("${spring.datasource.primary.driver-class-name}")
    private String driverClassName;

    @Bean
    @Primary
    public DataSource primaryDataSource() throws Exception {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        String decryptedPassword = DecryptionUtil.decrypt(password);
        dataSource.setPassword(decryptedPassword);
        dataSource.setDriverClassName(driverClassName);
        return dataSource;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
            @Qualifier("primaryDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.adp.benefits.carrier.entity.primary");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    @Primary
    public JpaTransactionManager primaryTransactionManager(
            @Qualifier("primaryEntityManagerFactory")
                    LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(entityManagerFactory.getObject()));
    }
}
