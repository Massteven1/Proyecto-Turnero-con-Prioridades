package com.banco.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /api/v1/turnos (contrato del catalogo de DTOs #16).
// El cliente solo aporta el identificador, el servicio y la categoria: el
// codigo consecutivo, la fecha de ingreso y el estado los genera el servidor.
public class RegistroTurnoDTO {

    @NotBlank(message = "El identificador del cliente es obligatorio")
    @Size(min = 3, max = 100, message = "El identificador del cliente debe tener entre 3 y 100 caracteres")
    private String identificadorCliente;

    @NotNull(message = "El servicio es obligatorio")
    @Positive(message = "El servicio debe ser un id válido")
    private Long servicioId;

    @NotNull(message = "La categoría es obligatoria")
    @Positive(message = "La categoría debe ser un id válido")
    private Long categoriaId;

    public String getIdentificadorCliente() {
        return identificadorCliente;
    }

    public void setIdentificadorCliente(String identificadorCliente) {
        this.identificadorCliente = identificadorCliente;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }
}