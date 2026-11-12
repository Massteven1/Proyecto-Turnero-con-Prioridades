package com.banco.app.dto.request;

import com.banco.app.entity.RolUsuario;
import com.banco.app.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /api/v1/usuarios (contrato del catalogo de DTOs #3):
// username 3-50 sin espacios, nombreCompleto 3-100, password 8-72 con
// maximo 72 bytes UTF-8 y rol ADMIN u OPERADOR obligatorio.
public class UsuarioCreateDTO {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^\\S+$", message = "El username no debe contener espacios")
    private String username;

    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(min = 3, max = 100, message = "El nombre completo debe tener entre 3 y 100 caracteres")
    private String nombreCompleto;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "La contraseña no debe exceder 72 bytes en UTF-8")
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private RolUsuario rol;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

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
}
