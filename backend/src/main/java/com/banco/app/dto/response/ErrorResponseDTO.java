package com.banco.app.dto.response;

import com.banco.app.config.ZonaHoraria;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

// Estructura unica de error del API (contrato del catalogo de DTOs #26):
// la generan @RestControllerAdvice/@ExceptionHandler y los componentes de
// seguridad (filtro de revalidacion, entry point y access denied handler).
// Para 400 de validacion, detallesValidacion trae el detalle campo por campo.
public class ErrorResponseDTO {

    private LocalDateTime timestamp;
    private Integer status;
    private String error;
    private String mensaje;
    private String ruta;
    private Map<String, String> detallesValidacion;

    public static ErrorResponseDTO de(HttpStatus estado, String mensaje, String ruta, Map<String, String> detallesValidacion) {
        ErrorResponseDTO respuesta = new ErrorResponseDTO();
        respuesta.timestamp = LocalDateTime.now(ZonaHoraria.ZONA);
        respuesta.status = estado.value();
        respuesta.error = estado.getReasonPhrase();
        respuesta.mensaje = mensaje;
        respuesta.ruta = ruta;
        respuesta.detallesValidacion = detallesValidacion;
        return respuesta;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getRuta() {
        return ruta;
    }

    public void setRuta(String ruta) {
        this.ruta = ruta;
    }

    public Map<String, String> getDetallesValidacion() {
        return detallesValidacion;
    }

    public void setDetallesValidacion(Map<String, String> detallesValidacion) {
        this.detallesValidacion = detallesValidacion;
    }
}
