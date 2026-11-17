package com.banco.app.dto.response;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Salida de GET /api/v1/turnos?modo=COLA (catalogo de DTOs #22).
// - ordenEnCola es la posicion PROYECTADA: el orden en que el algoritmo vigente
//   llamaria estos turnos si los modulos quedaran libres y no ingresara nadie
//   mas. No altera el contador persistido.
// - preferenteOriginal sale de la categoria; envejecido y preferenteEfectivo se
//   calculan con la hora unica de la consulta y el umbral del servicio.
// - tiempoEsperaMin se mide contra esa misma hora de consulta.
// - identificadorCliente viaja porque el endpoint es protegido por JWT; la
//   pantalla publica usa sus propios DTO sin datos del cliente.
public class TurnoColaDTO {

    private Integer ordenEnCola;
    private Long id;
    private String codigo;
    private Long servicioId;
    private String servicioNombre;
    private String identificadorCliente;
    private Long categoriaId;
    private String categoriaOriginalNombre;
    private Boolean preferenteOriginal;
    private Boolean preferenteEfectivo;
    private Boolean envejecido;
    private String conjuntoEfectivo;
    private LocalDate fechaOperacion;
    private LocalDateTime fechaIngreso;
    private Long tiempoEsperaMin;
    private EstadoTurno estado;

    public static TurnoColaDTO de(Turno turno, int ordenEnCola, boolean preferenteOriginal,
            boolean envejecido, LocalDateTime ahora) {
        TurnoColaDTO dto = new TurnoColaDTO();
        dto.ordenEnCola = ordenEnCola;
        dto.id = turno.getId();
        dto.codigo = turno.getCodigo();
        dto.servicioId = turno.getServicioId();
        dto.servicioNombre = turno.getServicio() != null ? turno.getServicio().getNombre() : null;
        dto.identificadorCliente = turno.getIdentificadorCliente();
        dto.categoriaId = turno.getCategoriaId();
        dto.categoriaOriginalNombre = turno.getCategoriaNombreSnapshot();
        dto.preferenteOriginal = preferenteOriginal;
        dto.preferenteEfectivo = preferenteOriginal || envejecido;
        dto.envejecido = envejecido;
        dto.conjuntoEfectivo = preferenteOriginal
                ? "PREFERENTE"
                : (envejecido ? "PREFERENTE_ENVEJECIDO" : "REGULAR");
        dto.fechaOperacion = turno.getFechaOperacion();
        dto.fechaIngreso = turno.getFechaIngreso();
        dto.tiempoEsperaMin = Duration.between(turno.getFechaIngreso(), ahora).toMinutes();
        dto.estado = turno.getEstado();
        return dto;
    }

    public Integer getOrdenEnCola() {
        return ordenEnCola;
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

    public String getIdentificadorCliente() {
        return identificadorCliente;
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

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public Long getTiempoEsperaMin() {
        return tiempoEsperaMin;
    }

    public EstadoTurno getEstado() {
        return estado;
    }
}
