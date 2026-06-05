package com.hotelos.maintenance.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("HotelOS Maintenance Service API")
                .description("API for managing maintenance requests, technician assignments, and priority-based scheduling")
                .version("1.0.0"))
            .servers(List.of(
                new Server().url("/").description("Default via Gateway"),
                new Server().url("http://localhost:8084").description("Direct Access")
            ));
    }
}
