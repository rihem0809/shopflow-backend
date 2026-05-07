package com.shopflow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ShopFlow API")
                        .version("1.0")
                        .description("API pour la plateforme e-commerce ShopFlow\n\n" +
                                "## Fonctionnalités :\n" +
                                "- Authentification JWT\n" +
                                "- Gestion des produits\n" +
                                "- Panier d'achat\n" +
                                "- Gestion des commandes\n" +
                                "- Avis et notations")
                        .contact(new Contact()
                                .name("ShopFlow Team")
                                .email("support@shopflow.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .name("Bearer Authentication")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Entrez votre token JWT comme: Bearer eyJhbGciOiJIUzI1NiIs...")));
    }
}