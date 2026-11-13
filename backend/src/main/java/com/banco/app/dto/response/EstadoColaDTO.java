package com.banco.app.dto.response;

import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.Servicio;
import java.time.LocalDate;

// Salida de los parametros de cola (GET y PUT, contrato del DTO #15).
public class EstadoColaDTO {

    private Long servicioId;
    private String servicioPrefijo;
    private String servicioNombre;
    private LocalDate fechaOperacion;
    private Integer ultimoConsecutivo;
    private Integer contadorPreferentes;
    private Integer umbralEnvejecimientoMin;
    private Integer proporcionPreferente;

    public static EstadoColaDTO de(EstadoColaServicio estado, Servicio servicio) {
        EstadoColaDTO dto = new EstadoColaDTO();
        dto.servicioId = estado.getServicioId();
        dto.servicioPrefijo = servicio != null ? servicio.getCodigoPrefijo() : null;
        dto.servicioNombre = servicio != null ? servicio.getNombre() : null;
        dto.fechaOperacion = estado.getFechaOperacion();
        dto.ultimoConsecutivo = estado.getUltimoConsecutivo();
        dto.contadorPreferentes = estado.getContadorPreferentes();
        dto.umbralEnvejecimientoMin = estado.getUmbralEnvejecimientoMin();
        dto.proporcionPreferente = estado.getProporcionPreferente();
        return dto;
    }

    public static EstadoColaDTO de(EstadoColaServicio estado) {
        return de(estado, estado.getServicio());
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public String getServicioPrefijo() {
        return servicioPrefijo;
    }

    public void setServicioPrefijo(String servicioPrefijo) {
        this.servicioPrefijo = servicioPrefijo;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public void setServicioNombre(String servicioNombre) {
        this.servicioNombre = servicioNombre;
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