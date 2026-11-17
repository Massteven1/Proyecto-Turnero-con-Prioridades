package com.banco.app.dto.response;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.HistorialTurno;
import java.time.LocalDateTime;

// Entrada cronologica del historial de un turno (catalogo de DTOs #28).
// - estadoAnterior es null solo en la emision inicial (-> EN_ESPERA).
// - operadorUsername es el ACTOR que ejecuto la transicion, no el operador
//   asignado al turno: el ADMIN que resuelve un turno ajeno queda aqui.
// - La tabla historial_turnos es inmutable: este DTO nunca se reconstruye.
public class TransicionHistorialDTO {

    private Long id;
    private EstadoTurno estadoAnterior;
    private EstadoTurno estadoNuevo;
    private LocalDateTime fechaCambio;
    private String operadorUsername;
    private String motivo;

    public static TransicionHistorialDTO de(HistorialTurno historial) {
        TransicionHistorialDTO dto = new TransicionHistorialDTO();
        dto.id = historial.getId();
        dto.estadoAnterior = historial.getEstadoAnterior();
        dto.estadoNuevo = historial.getEstadoNuevo();
        dto.fechaCambio = historial.getFechaCambio();
        dto.operadorUsername = historial.getOperador() != null
                ? historial.getOperador().getUsername()
                : null;
        dto.motivo = historial.getMotivo();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public EstadoTurno getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoTurno getEstadoNuevo() {
        return estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public String getOperadorUsername() {
        return operadorUsername;
    }

    public String getMotivo() {
        return motivo;
    }
}
