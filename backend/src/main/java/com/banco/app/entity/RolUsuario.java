package com.banco.app.entity;

// Roles autenticados del personal bancario.
// La consulta publica de la sala de espera es anonima y NO es un rol:
// no se guarda como usuario ni viaja dentro del JWT.
public enum RolUsuario {
    ADMIN,
    OPERADOR
}
