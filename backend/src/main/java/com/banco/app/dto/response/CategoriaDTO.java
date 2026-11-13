package com.banco.app.dto.response;

import com.banco.app.entity.Categoria;

// Salida del catalogo de categorias (GET y PUT, contrato del DTO #13).
public class CategoriaDTO {

    private Long id;
    private String nombre;
    private Boolean preferente;
    private Boolean activo;

    public static CategoriaDTO de(Categoria categoria) {
        CategoriaDTO dto = new CategoriaDTO();
        dto.id = categoria.getId();
        dto.nombre = categoria.getNombre();
        dto.preferente = categoria.getPreferente();
        dto.activo = categoria.getActivo();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getPreferente() {
        return preferente;
    }

    public void setPreferente(Boolean preferente) {
        this.preferente = preferente;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}