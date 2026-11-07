package com.eiu.pdds.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EIU Plant Disease Detection System API")
                        .description("Backend Core REST API documentation for the EIU Plant Disease Detection System (EIU PDDS). " +
                                "Provides endpoints for leaf disease diagnosis, authentication & identity, disease catalog management, and treatment protocols.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Thang (Backend Lead) & Huy (Client Lead)")
                                .email("dev@eiu.edu.vn"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("http://192.168.1.100:8080").description("LAN Dev Server (Expo Client)")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT Access Token (without 'Bearer ' prefix)")))
                .tags(List.of(
                        new Tag().name("Authentication & Identity").description("Endpoints for user registration, authentication, Google OAuth2 token exchange, token rotation, and profile management"),
                        new Tag().name("Diagnosis & AI Inference").description("Endpoints for leaf image upload, object detection inference, and treatment recommendations"),
                        new Tag().name("Disease Catalog").description("Endpoints for plant disease catalog, morphological symptoms, and agricultural treatments"),
                        new Tag().name("Plant Catalog").description("Endpoints for querying supported agricultural crops and botanical data")
                ));
    }
}
