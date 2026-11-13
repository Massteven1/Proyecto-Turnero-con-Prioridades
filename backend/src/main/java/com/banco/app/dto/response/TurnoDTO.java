package com.banco.app.dto.response;

import com.banco.app.entity.Categoria;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.Servicio;
import com.banco.app.entity.Turno;
import com.banco.app.entity.Usuario;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Salida de los tres endpoints de turnos (contrato del catalogo de DTOs #19):
// POST /api/v1/turnos, POST /api/v1/modulos/{id}/siguiente y
// PATCH /api/v1/turnos/{id}/estado.
// - categoriaOriginalNombre es la instantanea historica guardada al emitir,
//   nunca el nombre actual del catalogo.
// - preferenteOriginal sale de la categoria; envejecido y preferenteEfectivo
//   se calculan con la hora del servidor y el umbral vigente del servicio.
// - conjuntoEfectivo usa los valores del contrato:
//   PREFERENTE | PREFERENTE_ENVEJECIDO | REGULAR.
// - identificadorCliente solo viaja en respuestas protegidas por JWT.
public class TurnoDTO {

    private Long id;
    private String codigo;
    private Long servicioId;
    private String servicioNombre;
    private Long categoriaId;
    private String categoriaOriginalNombre;
    private Boolean preferenteOriginal;
    private Boolean preferenteEfectivo;
    private Boolean envejecido;
    private String conjuntoEfectivo;
    private Long moduloId;
    private String moduloNombre;
    private Long operadorId;
    private String operadorUsername;
    private LocalDate fechaOperacion;
    private Integer consecutivo;
    private String identificadorCliente;
    private EstadoTurno estado;
    private LocalDateTime fechaIngreso;
    private LocalDateTime fechaLlamado;
    private LocalDateTime fechaInicioAtencion;
    private LocalDateTime fechaFinAtencion;
    private LocalDateTime fechaCancelacionAusencia;
    private String motivoCancelacionAusencia;

    public static TurnoDTO de(Turno turno, Servicio servicio, Categoria categoria,
            ModuloAtencion modulo, Usuario operador, int umbralEnvejecimientoMin, LocalDateTime ahora) {
        TurnoDTO dto = new TurnoDTO();
        dto.id = turno.getId();
        dto.codigo = turno.getCodigo();
        dto.servicioId = turno.getServicioId();
        dto.servicioNombre = servicio != null ? servicio.getNombre() : null;
        dto.categoriaId = turno.getCategoriaId();
        dto.categoriaOriginalNombre = turno.getCategoriaNombreSnapshot();

        boolean preferente = categoria != null && Boolean.TRUE.equals(categoria.getPreferente());
        long esperaMinutos = Duration.between(turno.getFechaIngreso(), ahora).toMinutes();
        boolean envejecido = !preferente && esperaMinutos >= umbralEnvejecimientoMin;

        dto.preferenteOriginal = preferente;
        dto.envejecido = envejecido;
        dto.preferenteEfectivo = preferente || envejecido;
        dto.conjuntoEfectivo = preferente
                ? "PREFERENTE"
                : (envejecido ? "PREFERENTE_ENVEJECIDO" : "REGULAR");

        dto.moduloId = turno.getModuloId();
        dto.moduloNombre = modulo != null ? modulo.getNombre() : null;
        dto.operadorId = turno.getOperadorId();
        dto.operadorUsername = operador != null ? operador.getUsername() : null;
        dto.fechaOperacion = turno.getFechaOperacion();
        dto.consecutivo = turno.getConsecutivo();
        dto.identificadorCliente = turno.getIdentificadorCliente();
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

    public Long getServicioId() {
        return servicioId;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public String getCategoriaOriginalNombre() {
        return categoriaOriginalNombre;
    }

    public Boolean getPreferenteOriginal() {
        return preferenteOriginal;
    }

    public Boolean getPreferenteEfectivo() {
        return preferenteEfectivo;
    }

    public Boolean getEnvejecido() {
        return envejecido;
    }

    public String getConjuntoEfectivo() {
        return conjuntoEfectivo;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public String getModuloNombre() {
        return moduloNombre;
    }

    public Long getOperadorId() {
        return operadorId;
    }

    public String getOperadorUsername() {
        return operadorUsername;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public Integer getConsecutivo() {
        return consecutivo;
    }

    public String getIdentificadorCliente() {
        return identificadorCliente;
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