package com.banco.app.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// Produccion: con SWAGGER_ENABLED=false las propiedades de springdoc quedan en
// false y ni Swagger UI ni el contrato OpenAPI se publican.
@SpringBootTest(properties = {
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
class SwaggerDeshabilitadoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("con Swagger apagado /v3/api-docs y Swagger UI responden 404")
    void lasRutasDeSwaggerNoExisten() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isNotFound());
    }
}
