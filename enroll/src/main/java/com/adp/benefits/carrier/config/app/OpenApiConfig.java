package com.adp.benefits.carrier.config.app;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class OpenApiConfig {

    @Value("${app.name}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    private final Environment environment;

    public OpenApiConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();
        servers.add(new Server().url("").description("Local environment"));

        if (Arrays.asList(environment.getActiveProfiles()).contains("dit")
                || Arrays.asList(environment.getActiveProfiles()).contains("fit")) {
            servers.add(
                    new Server()
                            .url("/baas-client-process-manager")
                            .description("AWS environment"));
        }

        return new OpenAPI()
                .info(
                        new Info()
                                .title(appName)
                                .version(appVersion)
                                .description("BAAS Client Process Manager API Documentation")
                                .contact(new Contact().name("K2: ADP Benefits Team")))
                .servers(servers);
    }

    @Bean
    public GroupedOpenApi apiGroup() {
        return GroupedOpenApi.builder()
                .group("baas-api")
                .pathsToMatch("/v1/api/**")
                .displayName("BAAS Client Process Manager")
                .build();
    }
}
