package com.banco.app.dto.response;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Salida de GET /api/v1/turnos?modo=HISTORIAL (catalogo de DTOs #23).
// - categoriaOriginalNombre es el nombre historico guardado al emitir el turno,
//   nunca el nombre actual del catalogo.
// - identificadorCliente viaja porque el historial es exclusivo de ADMIN.
// - El detalle con transiciones es otro endpoint (TurnoDetalleDTO).
public class TurnoResumenDTO {

    private Long id;
    private String codigo;
    private LocalDate fechaOperacion;
    private String servicioNombre;
    private String categoriaOriginalNombre;
    private String identificadorCliente;
    private String moduloNombre;
    private String operadorUsername;
    private EstadoTurno estado;
    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaLlamado;
    private LocalDateTime fechaInicioAtencion;
    private LocalDateTime fechaFinAtencion;
    private LocalDateTime fechaCancelacionAusencia;
    private String motivoCancelacionAusencia;

    public static TurnoResumenDTO de(Turno turno) {
        TurnoResumenDTO dto = new TurnoResumenDTO();
        dto.id = turno.getId();
        dto.codigo = turno.getCodigo();
        dto.fechaOperacion = turno.getFechaOperacion();
        dto.servicioNombre = turno.getServicio() != null ? turno.getServicio().getNombre() : null;
        dto.categoriaOriginalNombre = turno.getCategoriaNombreSnapshot();
        dto.identificadorCliente = turno.getIdentificadorCliente();
        dto.moduloNombre = turno.getModulo() != null ? turno.getModulo().getNombre() : null;
        dto.operadorUsername = turno.getOperador() != null ? turno.getOperador().getUsername() : null;
        dto.estado = turno.getEstado();
        dto.fechaIngreso = turno.getFechaIngreso();
        dto.fechaLlamado = turno.getFechaLlamado();
        dto.fechaInicioAtencion = turno.getFechaInicioAtencion();
        dto.fechaFinAtencion = turno.getFechaFinAtencion();
        dto.fechaCancelacionAusencia = turno.getFechaCancelacionAusencia();
        dto.motivoCancelacionAusencia = turno.getMotivoCancelacionAusencia();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public String getCategoriaOriginalNombre() {
        return categoriaOriginalNombre;
    }

    public String getIdentificadorCliente() {
        return identificadorCliente;
    }

    public String getModuloNombre() {
        return moduloNombre;
    }

    public String getOperadorUsername() {
        return operadorUsername;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public LocalDateTime getFechaLlamado() {
        return fechaLlamado;
    }

    public LocalDateTime getFechaInicioAtencion() {
        return fechaInicioAtencion;
    }

    public LocalDateTime getFechaFinAtencion() {
        return fechaFinAtencion;
    }

    public LocalDateTime getFechaCancelacionAusencia() {
        return fechaCancelacionAusencia;
    }

    public String getMotivoCancelacionAusencia() {
        return motivoCancelacionAusencia;
    }
}
