package com.banco.app.dto.response;

import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Usuario;

// Salida del CRUD de usuarios (GET, POST y PUT, contrato del catalogo #5).
// Excluye deliberadamente passwordHash: el hash jamas sale por el API.
public class UsuarioDTO {

    private Long id;
    private String username;
    private String nombreCompleto;
    private RolUsuario rol;
    private Boolean activo;

    public static UsuarioDTO de(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.id = usuario.getId();
        dto.username = usuario.getUsername();
        dto.nombreCompleto = usuario.getNombreCompleto();
        dto.rol = usuario.getRol();
        dto.activo = usuario.getActivo();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
