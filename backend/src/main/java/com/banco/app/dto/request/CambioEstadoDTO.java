package com.banco.app.dto.request;

import com.banco.app.entity.EstadoTurno;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cuerpo del PATCH /api/v1/turnos/{id}/estado (contrato del catalogo de DTOs #17).
// nuevoEstado solo admite los destinos operativos: EN_ATENCION, FINALIZADO,
// CANCELADO o AUSENTE (la emision y el llamado no se hacen por este endpoint).
// El motivo es obligatorio, de 5 a 255 caracteres, para CANCELADO y AUSENTE:
// la condicion depende de nuevoEstado, asi que la valida TurnoService para
// poder devolver 400 Bad Request con un mensaje claro.
public class CambioEstadoDTO {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoTurno nuevoEstado;

    @Size(max = 255, message = "El motivo no debe exceder 255 caracteres")
    private String motivo;

    public EstadoTurno getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(EstadoTurno nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}