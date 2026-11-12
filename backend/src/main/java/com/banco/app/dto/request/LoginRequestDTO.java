package com.banco.app.dto.request;

import com.banco.app.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cuerpo del POST /api/v1/auth/login (contrato del catalogo de DTOs):
// username 3-50 caracteres, password 8-72 caracteres y maximo 72 bytes UTF-8.
public class LoginRequestDTO {

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @MaxUtf8Bytes(value = 72, message = "La contraseña no debe exceder 72 bytes en UTF-8")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
