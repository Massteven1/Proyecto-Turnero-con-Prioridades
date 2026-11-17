package com.banco.app.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

// Limite de intentos fallidos de login por username.
// - Se cuentan solo los fallos y el contador se limpia con un login correcto.
// - La clave se normaliza (trim + minusculas) para que no se esquive cambiando
//   mayusculas del username.
// - Vive en memoria: alcanza para una instancia, que es el alcance del proyecto.
//   Con varias instancias haria falta un almacen compartido (Redis, tabla).
// - Se consulta ANTES de tocar la base: un atacante no llega a BCrypt ni al
//   repositorio mientras la cuenta esta bloqueada.
@Component
public class LoginThrottle {

    // Poda defensiva: si entran muchos usernames distintos, se limpian las
    // ventanas vencidas para que el mapa no crezca sin control.
    private static final int ENTRADAS_MAXIMAS = 10_000;

    private final int maxIntentos;
    private final Duration ventana;
    private final Map<String, Intento> intentos = new ConcurrentHashMap<>();

    public LoginThrottle(
            @Value("${app.login.max-intentos:5}") int maxIntentos,
            @Value("${app.login.ventana-minutos:15}") long ventanaMinutos) {
        this.maxIntentos = maxIntentos;
        this.ventana = Duration.ofMinutes(ventanaMinutos);
    }

    // 429 con el tiempo restante cuando el username agoto sus intentos.
    public void verificarPermitido(String username) {
        Intento intento = intentos.get(clave(username));
        if (intento == null || ventanaVencida(intento) || intento.fallos() < maxIntentos) {
            return;
        }
        long minutos = Math.max(1,
                Duration.between(Instant.now(), intento.inicio().plus(ventana)).toMinutes() + 1);
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos fallidos; intente de nuevo en " + minutos + " minuto(s)");
    }

    public void registrarFallo(String username) {
        podarSiHaceFalta();
        intentos.compute(clave(username), (clave, previo) -> {
            if (previo == null || ventanaVencida(previo)) {
                return new Intento(Instant.now(), 1);
            }
            return new Intento(previo.inicio(), previo.fallos() + 1);
        });
    }

    public void registrarExito(String username) {
        intentos.remove(clave(username));
    }

    private boolean ventanaVencida(Intento intento) {
        return !Instant.now().isBefore(intento.inicio().plus(ventana));
    }

    private void podarSiHaceFalta() {
        if (intentos.size() >= ENTRADAS_MAXIMAS) {
            intentos.values().removeIf(this::ventanaVencida);
        }
    }

    private static String clave(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private record Intento(Instant inicio, int fallos) {
    }
}
