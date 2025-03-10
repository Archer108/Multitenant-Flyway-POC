package com.poc.flyway.Multitenant_Flyway_POC;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})
public class MultitenantFlywayPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultitenantFlywayPocApplication.class, args);
    }
}
