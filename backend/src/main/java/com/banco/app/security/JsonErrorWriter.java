package com.banco.app.security;

import com.banco.app.dto.response.ErrorResponseDTO;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

// Escritor de ErrorResponseDTO en JSON para los componentes que corren
// ANTES de la capa MVC (filtro y handlers de seguridad): ahi no aplica el
// @RestControllerAdvice, asi que se serializa manualmente con el ObjectMapper
// de Spring Boot (que ya sabe serializar LocalDateTime).
final class JsonErrorWriter {

    private JsonErrorWriter() {
        // Utilidad de metodos estaticos: no se instancia
    }

    static void escribir(HttpServletResponse response, ObjectMapper mapper, HttpStatus estado, String mensaje, String ruta) throws java.io.IOException {
        response.setStatus(estado.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), ErrorResponseDTO.de(estado, mensaje, ruta, null));
    }
}
