package com.banco.app.dto.response;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.LocalDateTime;

// Turno que ya ocupa un modulo en la pantalla publica (LLAMADO o EN_ATENCION).
// Sin datos personales: la sala de espera solo muestra el codigo y la ventanilla.
public class TurnoPublicoModuloDTO {

    private String codigo;
    private String servicioNombre;
    private String moduloNombre;
    private EstadoTurno estado;
    private LocalDateTime fechaLlamado;

    public static TurnoPublicoModuloDTO de(Turno turno) {
        TurnoPublicoModuloDTO dto = new TurnoPublicoModuloDTO();
        dto.codigo = turno.getCodigo();
        dto.servicioNombre = turno.getServicio() != null ? turno.getServicio().getNombre() : null;
        dto.moduloNombre = turno.getModulo() != null ? turno.getModulo().getNombre() : null;
        dto.estado = turno.getEstado();
        dto.fechaLlamado = turno.getFechaLlamado();
        return dto;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public String getModuloNombre() {
        return moduloNombre;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public LocalDateTime getFechaLlamado() {
        return fechaLlamado;
    }
}
