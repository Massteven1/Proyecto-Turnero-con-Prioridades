package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

// Auditoria inmutable de las transiciones de estado de cada turno.
// Cada emision y cada cambio de estado se registra aqui con el usuario
// autenticado que ejecuto la operacion (el ACTOR, extraido del JWT):
// no confundir operador_id con turnos.operador_id, que es el operador ASIGNADO al turno.
// estado_anterior es NULL solo en la transicion inicial de emision (-> EN_ESPERA).
// La tabla es inmutable: los triggers trg_historial_turnos_no_update y
// trg_historial_turnos_no_delete lo impiden incluso por SQL.
@Entity
@Table(name = "historial_turnos")
public class HistorialTurno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_id", nullable = false)
    private Long turnoId;

    // Actor que ejecuto la transicion (usuarioId del JWT)
    @Column(name = "operador_id", nullable = false)
    private Long operadorId;

    // Null solo en la emision inicial; si no, debe pertenecer al catalogo de estados
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior")
    private EstadoTurno estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false)
    private EstadoTurno estadoNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    // Motivo de la cancelacion/ausencia, o texto opcional de otras transiciones
    @Column(name = "motivo")
    private String motivo;

    // ---- Relaciones solo lectura para los DTO ----
    @ManyToOne
    @JoinColumn(name = "turno_id", insertable = false, updatable = false)
    private Turno turno;

    @ManyToOne
    @JoinColumn(name = "operador_id", insertable = false, updatable = false)
    private Usuario operador;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTurnoId() {
        return turnoId;
    }

    public void setTurnoId(Long turnoId) {
        this.turnoId = turnoId;
    }

    public Long getOperadorId() {
        return operadorId;
    }

    public void setOperadorId(Long operadorId) {
        this.operadorId = operadorId;
    }

    public EstadoTurno getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(EstadoTurno estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public EstadoTurno getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(EstadoTurno estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Turno getTurno() {
        return turno;
    }

    public Usuario getOperador() {
        return operador;
    }
}
