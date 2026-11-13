package com.banco.app.controller;

import com.banco.app.config.Roles;
import com.banco.app.dto.request.CambioEstadoDTO;
import com.banco.app.dto.request.RegistroTurnoDTO;
import com.banco.app.dto.response.TurnoDTO;
import com.banco.app.security.Identidad;
import com.banco.app.service.TurnoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Emision y transiciones de turnos (contrato de las filas 19 y 21).
// El actor se extrae del JWT: nunca se acepta un operadorId enviado por el cliente.
@RestController
@RequestMapping("/api/v1/turnos")
public class TurnoController {

    private final TurnoService turnoService;

    public TurnoController(TurnoService turnoService) {
        this.turnoService = turnoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<TurnoDTO> registrar(@Valid @RequestBody RegistroTurnoDTO peticion,
            Authentication autenticacion) {
        TurnoDTO creado = turnoService.registrar(peticion,
                Identidad.usuarioId(autenticacion), Roles.esAdministrador(autenticacion));
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public TurnoDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoDTO peticion,
            Authentication autenticacion) {
        return turnoService.cambiarEstado(id, peticion,
                Identidad.usuarioId(autenticacion), Roles.esAdministrador(autenticacion));
    }
}