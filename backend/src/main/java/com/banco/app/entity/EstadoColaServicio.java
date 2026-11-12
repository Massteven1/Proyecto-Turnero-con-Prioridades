package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

// Control transaccional de la cola: una fila 1:1 por servicio (uk_estado_cola_servicio).
// Aqui vive el estado que debe SOBREVIVIR a reinicios del backend:
// - fecha_operacion: dia de operacion en America/Bogota; al cambiar de dia el backend
//   reinicia ultimo_consecutivo y contador_preferentes con la primera operacion.
// - ultimo_consecutivo: ultimo numero emitido del dia (el siguiente es este + 1).
// - contador_preferentes: llamados consecutivos del conjunto preferente efectivo,
//   entre 0 y proporcion_preferente (proporcion N:1, 3 por defecto).
// - umbral_envejecimiento_min: minutos de espera que "envejecen" un Regular (15 por defecto).
@Entity
@Table(name = "estado_cola_servicio")
public class EstadoColaServicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacion 1:1 estricta con servicios (UNIQUE servicio_id)
    @Column(name = "servicio_id", nullable = false, unique = true)
    private Long servicioId;

    // Solo lectura: para armar EstadoColaDTO con prefijo y nombre del servicio
    @ManyToOne
    @JoinColumn(name = "servicio_id", insertable = false, updatable = false)
    private Servicio servicio;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @Column(name = "ultimo_consecutivo", nullable = false)
    private Integer ultimoConsecutivo;

    @Column(name = "contador_preferentes", nullable = false)
    private Integer contadorPreferentes;

    @Column(name = "umbral_envejecimiento_min", nullable = false)
    private Integer umbralEnvejecimientoMin;

    @Column(name = "proporcion_preferente", nullable = false)
    private Integer proporcionPreferente;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(LocalDate fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public Integer getUltimoConsecutivo() {
        return ultimoConsecutivo;
    }

    public void setUltimoConsecutivo(Integer ultimoConsecutivo) {
        this.ultimoConsecutivo = ultimoConsecutivo;
    }

    public Integer getContadorPreferentes() {
        return contadorPreferentes;
    }

    public void setContadorPreferentes(Integer contadorPreferentes) {
        this.contadorPreferentes = contadorPreferentes;
    }

    public Integer getUmbralEnvejecimientoMin() {
        return umbralEnvejecimientoMin;
    }

    public void setUmbralEnvejecimientoMin(Integer umbralEnvejecimientoMin) {
        this.umbralEnvejecimientoMin = umbralEnvejecimientoMin;
    }

    public Integer getProporcionPreferente() {
        return proporcionPreferente;
    }

    public void setProporcionPreferente(Integer proporcionPreferente) {
        this.proporcionPreferente = proporcionPreferente;
    }
}
