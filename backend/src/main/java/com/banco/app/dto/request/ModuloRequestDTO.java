package com.banco.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /api/v1/modulos (contrato del catalogo de DTOs #9).
// El servicio destino debe existir y estar activo; el nombre es unico
// globalmente ignorando mayusculas/minusculas y espacios exteriores.
public class ModuloRequestDTO {

    @NotNull(message = "El servicio es obligatorio")
    @Positive(message = "El servicio debe ser un id válido")
    private Long servicioId;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 50, message = "El nombre debe tener entre 3 y 50 caracteres")
    private String nombre;

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
}