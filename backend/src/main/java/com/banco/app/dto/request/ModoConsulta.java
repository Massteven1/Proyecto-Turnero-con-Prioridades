package com.banco.app.dto.request;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Valor del query param ?modo= de GET /api/v1/turnos (contrato de la fila 24 del
// xlsx): COLA o HISTORIAL. Si el parametro se omite, el modo es COLA.
// Cualquier otro valor es 400 Bad Request.
public enum ModoConsulta {

    COLA,
    HISTORIAL;

    public static ModoConsulta desde(String valor) {
        if (valor == null) {
            return COLA;
        }
        try {
            return valueOf(valor.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El modo debe ser COLA o HISTORIAL");
        }
    }
}
