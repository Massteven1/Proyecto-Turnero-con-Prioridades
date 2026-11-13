package com.banco.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /api/v1/servicios (contrato del catalogo de DTOs #6).
// codigoPrefijo forma parte del codigo visible del turno (CAJ-20261105-001),
// por eso solo admite mayusculas de 2 a 10 caracteres y es inmutable despues.
public class ServicioRequestDTO {

    @NotBlank(message = "El código de prefijo es obligatorio")
    @Pattern(regexp = "^[A-Z]{2,10}$", message = "El código de prefijo debe tener entre 2 y 10 letras mayúsculas")
    private String codigoPrefijo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no debe exceder 255 caracteres")
    private String descripcion;

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
}