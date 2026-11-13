package com.banco.app.dto.response;

import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.Servicio;

// Salida del catalogo de modulos (GET, POST y PUT, contrato del DTO #11).
// ocupado y turnoActivoCodigo se calculan con el turno LLAMADO/EN_ATENCION
// vigente del modulo: si no hay turno activo, ambos quedan en false/null.
public class ModuloDTO {

    private Long id;
    private Long servicioId;
    private String servicioNombre;
    private String servicioPrefijo;
    private String nombre;
    private Boolean activo;
    private Boolean ocupado;
    private String turnoActivoCodigo;

    public static ModuloDTO de(ModuloAtencion modulo, Servicio servicio, String turnoActivoCodigo) {
        ModuloDTO dto = new ModuloDTO();
        dto.id = modulo.getId();
        dto.servicioId = modulo.getServicioId();
        dto.servicioNombre = servicio != null ? servicio.getNombre() : null;
        dto.servicioPrefijo = servicio != null ? servicio.getCodigoPrefijo() : null;
        dto.nombre = modulo.getNombre();
        dto.activo = modulo.getActivo();
        dto.turnoActivoCodigo = turnoActivoCodigo;
        dto.ocupado = turnoActivoCodigo != null;
        return dto;
    }

    public static ModuloDTO de(ModuloAtencion modulo, String turnoActivoCodigo) {
        return de(modulo, modulo.getServicio(), turnoActivoCodigo);
    }

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

    public String getServicioNombre() {
        return servicioNombre;
    }

    public void setServicioNombre(String servicioNombre) {
        this.servicioNombre = servicioNombre;
    }

    public String getServicioPrefijo() {
        return servicioPrefijo;
    }

    public void setServicioPrefijo(String servicioPrefijo) {
        this.servicioPrefijo = servicioPrefijo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Boolean getOcupado() {
        return ocupado;
    }

    public void setOcupado(Boolean ocupado) {
        this.ocupado = ocupado;
    }

    public String getTurnoActivoCodigo() {
        return turnoActivoCodigo;
    }

    public void setTurnoActivoCodigo(String turnoActivoCodigo) {
        this.turnoActivoCodigo = turnoActivoCodigo;
    }
}