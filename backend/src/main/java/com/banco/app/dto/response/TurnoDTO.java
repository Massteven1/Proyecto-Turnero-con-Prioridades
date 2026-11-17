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
        dto.completar(turno, servicio, categoria, modulo, operador, umbralEnvejecimientoMin, ahora);
        return dto;
    }

    // Mapeo compartido con TurnoDetalleDTO: el detalle es exactamente este DTO
    // mas las transiciones al mismo nivel, asi que no se duplican los campos.
    protected void completar(Turno turno, Servicio servicio, Categoria categoria,
            ModuloAtencion modulo, Usuario operador, int umbralEnvejecimientoMin, LocalDateTime ahora) {
        this.id = turno.getId();
        this.codigo = turno.getCodigo();
        this.servicioId = turno.getServicioId();
        this.servicioNombre = servicio != null ? servicio.getNombre() : null;
        this.categoriaId = turno.getCategoriaId();
        this.categoriaOriginalNombre = turno.getCategoriaNombreSnapshot();

        boolean preferente = categoria != null && Boolean.TRUE.equals(categoria.getPreferente());
        long esperaMinutos = Duration.between(turno.getFechaIngreso(), ahora).toMinutes();
        boolean envejecido = !preferente && esperaMinutos >= umbralEnvejecimientoMin;

        this.preferenteOriginal = preferente;
        this.envejecido = envejecido;
        this.preferenteEfectivo = preferente || envejecido;
        this.conjuntoEfectivo = preferente
                ? "PREFERENTE"
                : (envejecido ? "PREFERENTE_ENVEJECIDO" : "REGULAR");

        this.moduloId = turno.getModuloId();
        this.moduloNombre = modulo != null ? modulo.getNombre() : null;
        this.operadorId = turno.getOperadorId();
        this.operadorUsername = operador != null ? operador.getUsername() : null;
        this.fechaOperacion = turno.getFechaOperacion();
        this.consecutivo = turno.getConsecutivo();
        this.identificadorCliente = turno.getIdentificadorCliente();
        this.estado = turno.getEstado();
        this.fechaIngreso = turno.getFechaIngreso();
        this.fechaLlamado = turno.getFechaLlamado();
        this.fechaInicioAtencion = turno.getFechaInicioAtencion();
        this.fechaFinAtencion = turno.getFechaFinAtencion();
        this.fechaCancelacionAusencia = turno.getFechaCancelacionAusencia();
        this.motivoCancelacionAusencia = turno.getMotivoCancelacionAusencia();
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