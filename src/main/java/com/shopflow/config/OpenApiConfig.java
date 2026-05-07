package com.shopflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration  // ceci est une classe de configuration
@OpenAPIDefinition(  // Configure la documentation Swagger/OpenAPI
        info = @Info(title = "ShopFlow API", version = "1.0"),  // Titre et version de l'API
        security = @SecurityRequirement(name = "Bearer Authentication")  // Tous les endpoints nécessitent un token JWT par défaut
)
@SecurityScheme(  // Définit comment envoyer le token dans Swagger
        name = "Bearer Authentication",  // Nom du schéma de sécurité
        type = SecuritySchemeType.HTTP,   // Type HTTP
        scheme = "bearer",                // Utilise le schéma Bearer
        bearerFormat = "JWT"              // Le token est au format JWT
)
public class OpenApiConfig {
    // Cette classe permet aux développeurs de tester l'API depuis Swagger en ajoutant un bouton "Authorize"
}