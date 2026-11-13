package com.banco.app.dto.response;

import com.banco.app.entity.Servicio;

// Salida del catalogo de servicios (GET, POST y PUT, contrato del DTO #8).
public class ServicioDTO {

    private Long id;
    private String codigoPrefijo;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    public static ServicioDTO de(Servicio servicio) {
        ServicioDTO dto = new ServicioDTO();
        dto.id = servicio.getId();
        dto.codigoPrefijo = servicio.getCodigoPrefijo();
        dto.nombre = servicio.getNombre();
        dto.descripcion = servicio.getDescripcion();
        dto.activo = servicio.getActivo();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoPrefijo() {
        return codigoPrefijo;
    }

    public void setCodigoPrefijo(String codigoPrefijo) {
        this.codigoPrefijo = codigoPrefijo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}