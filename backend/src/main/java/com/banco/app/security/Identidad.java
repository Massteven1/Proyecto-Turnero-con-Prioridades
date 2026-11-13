package com.banco.app.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

// Identidad del actor autenticado. El contrato exige que el usuario de una
// operacion salga EXCLUSIVAMENTE del JWT (sub = usuarioId) y nunca del cuerpo
// ni de la ruta: los controladores usan este helper en lugar de aceptar ids
// enviados por el cliente.
public final class Identidad {

    private Identidad() {
        // Clase de utilidades: no se instancia
    }

    public static Long usuarioId(Authentication autenticacion) {
        if (autenticacion instanceof JwtAuthenticationToken token) {
            try {
                return Long.parseLong(token.getToken().getSubject());
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Se requiere un JWT válido para acceder a este recurso");
    }
}