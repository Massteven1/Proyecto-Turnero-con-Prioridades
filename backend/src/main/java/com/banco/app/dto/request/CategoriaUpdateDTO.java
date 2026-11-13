package com.banco.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo del PUT /api/v1/categorias/{id} (contrato del catalogo de DTOs #12).
// Solo se renombra: preferente y activo son inmutables en las categorias base,
// por eso no aparecen en el DTO.
public class CategoriaUpdateDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    private String nombre;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}