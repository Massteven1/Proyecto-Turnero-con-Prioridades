package com.banco.app.dto.request;

import com.banco.app.entity.RolUsuario;
import com.banco.app.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Cuerpo del PUT /api/v1/usuarios/{id} (contrato del catalogo de DTOs #4):
// el username NO se modifica, por eso no aparece en este DTO.
// - password: opcional/null, si se envia debe ser no vacio, 8-72 chars
//   y maximo 72 bytes UTF-8; null u omitido conserva el hash actual.
public class UsuarioUpdateDTO {

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre completo debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    // @Size deja pasar null (campo opcional) y rechaza "" o valores cortos
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "La contraseña no debe exceder 72 bytes en UTF-8")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    @NotNull(message = "El estado activo es obligatorio")
    private Boolean activo;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
