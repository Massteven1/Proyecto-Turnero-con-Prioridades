package com.banco.app.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Endurecimiento de la configuracion: el secreto se valida al construir la
// clave (arranque).
class SecurityConfigTest {

    @Test
    @DisplayName("un JWT_SECRET de menos de 32 bytes no permite arrancar")
    void rechazaUnSecretoCorto() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "secreto", "corto");

        assertThatThrownBy(config::jwtDecoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 32 bytes");
    }

    @Test
    @DisplayName("un JWT_SECRET vacío se reporta como no configurado")
    void rechazaUnSecretoVacio() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "secreto", "   ");

        assertThatThrownBy(config::jwtDecoder)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no está configurada");
    }

    @Test
    @DisplayName("un secreto de 32 bytes construye el decoder")
    void aceptaUnSecretoDe32Bytes() {
        SecurityConfig config = new SecurityConfig();
        ReflectionTestUtils.setField(config, "secreto", "0123456789abcdef0123456789abcdef");

        assertThat(config.jwtDecoder()).isNotNull();
        assertThat(config.jwtEncoder()).isNotNull();
    }
}
