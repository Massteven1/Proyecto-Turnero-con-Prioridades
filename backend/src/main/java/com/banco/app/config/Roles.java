package com.banco.app.config;

import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

// Utilidad para que los controladores adapten el alcance de una consulta al
// rol VIGENTE del JWT (el filtro ya revalido la cuenta en esta peticion).
public final class Roles {

    private Roles() {
        // Clase de utilidades: no se instancia
    }

    public static boolean esAdministrador(Authentication autenticacion) {
        return tieneRol(autenticacion, "ROLE_ADMIN");
    }

    private static boolean tieneRol(Authentication autenticacion, String rol) {
        if (autenticacion == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> autoridades = autenticacion.getAuthorities();
        return autoridades.stream().anyMatch(autoridad -> rol.equals(autoridad.getAuthority()));
    }
}