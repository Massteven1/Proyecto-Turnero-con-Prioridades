package com.banco.app.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Contrato OpenAPI que springdoc genera leyendo los controladores, los DTOs y
// las anotaciones de Jakarta Validation. El esquema bearerAuth habilita el
// boton "Authorize" de Swagger UI para pegar el JWT del login.
@Configuration
@SecurityScheme(
        name = OpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT emitido por POST /api/v1/auth/login")
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI turneroOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Turnero con Prioridades - API")
                .version("v1")
                .description("""
                        API REST del sistema de turnos bancarios con prioridades.
                        Todos los endpoints viven bajo /api/v1 y los errores usan
                        ErrorResponseDTO con timestamp, status, error, mensaje, ruta
                        y detallesValidacion. Autenticacion: JWT Bearer (HS256).
                        """));
    }
}