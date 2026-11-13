package com.banco.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Cuerpo del PUT /api/v1/modulos/{id} (contrato del catalogo de DTOs #10).
// servicioId solo puede cambiar si el modulo no tiene turnos asociados.
public class ModuloUpdateDTO {

    @NotNull(message = "El servicio es obligatorio")
    @Positive(message = "El servicio debe ser un id válido")
    private Long servicioId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    private String nombre;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
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
}