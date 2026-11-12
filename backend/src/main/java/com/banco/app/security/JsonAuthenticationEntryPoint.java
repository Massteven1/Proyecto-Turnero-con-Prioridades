package com.banco.app.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

// 401 cuando la peticion NO trae un JWT valido (ausente, mal firmado o expirado).
// Devuelve el ErrorResponseDTO JSON del contrato en lugar del cuerpo vacio por defecto.
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException excepcion)
            throws IOException {
        JsonErrorWriter.escribir(response, objectMapper, HttpStatus.UNAUTHORIZED,
                "Se requiere un JWT válido para acceder a este recurso", request.getRequestURI());
    }
}
