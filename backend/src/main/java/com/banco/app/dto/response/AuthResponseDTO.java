package com.banco.app.dto.response;

import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Usuario;
import com.banco.app.service.JwtService;
import java.time.LocalDateTime;

// Respuesta del POST /api/v1/auth/login (contrato del catalogo de DTOs).
// El token lleva sub=usuarioId, claim rol y exp; el filtro de seguridad
// revalida la cuenta en cada peticion protegida (activo y rol vigente).
public class AuthResponseDTO {

    private String token;
    private String tipoToken;
    private LocalDateTime expiracion;
    private Long usuarioId;
    private String username;
    private String nombreCompleto;
    private RolUsuario rol;

    public static AuthResponseDTO de(Usuario usuario, JwtService.TokenEmitido emitido) {
        AuthResponseDTO respuesta = new AuthResponseDTO();
        respuesta.token = emitido.token();
        respuesta.tipoToken = "Bearer";
        respuesta.expiracion = emitido.expiracion();
        respuesta.usuarioId = usuario.getId();
        respuesta.username = usuario.getUsername();
        respuesta.nombreCompleto = usuario.getNombreCompleto();
        respuesta.rol = usuario.getRol();
        return respuesta;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public void setTipoToken(String tipoToken) {
        this.tipoToken = tipoToken;
    }

    public LocalDateTime getExpiracion() {
        return expiracion;
    }

    public void setExpiracion(LocalDateTime expiracion) {
        this.expiracion = expiracion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

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

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }
}
