package com.banco.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

// Limite de intentos fallidos de login: cuenta, limpia y vence la ventana.
class LoginThrottleTest {

    @Test
    @DisplayName("permite hasta el máximo y después responde 429")
    void permiteHastaElMaximo() {
        LoginThrottle throttle = new LoginThrottle(2, 15);

        throttle.registrarFallo("admin");
        throttle.verificarPermitido("admin");
        throttle.registrarFallo("admin");

        ResponseStatusException error = catchThrowableOfType(
                () -> throttle.verificarPermitido("admin"), ResponseStatusException.class);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(error.getReason()).contains("Demasiados intentos fallidos");
    }

    @Test
    @DisplayName("un login correcto limpia los intentos acumulados")
    void elExitoLimpiaElContador() {
        LoginThrottle throttle = new LoginThrottle(2, 15);

        throttle.registrarFallo("admin");
        throttle.registrarFallo("admin");
        throttle.registrarExito("admin");
        throttle.registrarFallo("admin");

        throttle.verificarPermitido("admin");
    }

    @Test
    @DisplayName("al vencer la ventana el contador arranca de nuevo")
    void laVentanaVencidaReiniciaElContador() {
        // Ventana de 0 minutos: cualquier intento previo ya está vencido.
        LoginThrottle throttle = new LoginThrottle(1, 0);

        throttle.registrarFallo("admin");
        throttle.registrarFallo("admin");

        throttle.verificarPermitido("admin");
    }

    @Test
    @DisplayName("el username no se esquiva cambiando mayúsculas")
    void elUsernameNoDistingueMayusculas() {
        LoginThrottle throttle = new LoginThrottle(1, 15);

        throttle.registrarFallo("  Admin ");

        ResponseStatusException error = catchThrowableOfType(
                () -> throttle.verificarPermitido("admin"), ResponseStatusException.class);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
