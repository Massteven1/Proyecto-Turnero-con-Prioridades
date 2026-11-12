package com.banco.app.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

// 403 cuando el JWT es valido pero el rol no tiene permiso para la ruta
// (ej.: un OPERADOR pidiendo GET /api/v1/usuarios que exige ADMIN).
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException excepcion)
            throws IOException {
        JsonErrorWriter.escribir(response, objectMapper, HttpStatus.FORBIDDEN,
                "La cuenta no tiene permisos para este recurso", request.getRequestURI());
    }
}
