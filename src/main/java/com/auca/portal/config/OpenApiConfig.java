package com.auca.portal.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi contractApi() {
        return GroupedOpenApi.builder()
                .group("contract-api")
                .pathsToMatch("/api/contract/**")
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("AUCA Portal Contract API")
                        .description("API for contract creation and management (contract-only)")
                        .version("1.0"));
    }
}