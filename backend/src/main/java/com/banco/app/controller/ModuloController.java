package com.banco.app.controller;

import com.banco.app.config.Roles;
import com.banco.app.dto.request.ModuloRequestDTO;
import com.banco.app.dto.request.ModuloUpdateDTO;
import com.banco.app.dto.response.ModuloDTO;
import com.banco.app.service.ModuloService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Catalogo de modulos o ventanillas. GET lo comparten ADMIN y OPERADOR;
// las mutaciones son exclusivas de ADMIN (403 para OPERADOR).
@RestController
@RequestMapping("/api/v1/modulos")
public class ModuloController {

    private final ModuloService moduloService;

    public ModuloController(ModuloService moduloService) {
        this.moduloService = moduloService;
    }

    // Filtros opcionales del contrato: servicioId para ADMIN y OPERADOR;
    // activo solo ADMIN (OPERADOR con activo=false recibe 403).
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public List<ModuloDTO> listar(@RequestParam(name = "servicioId", required = false) Long servicioId,
            @RequestParam(name = "activo", required = false) Boolean activo,
            Authentication autenticacion) {
        return moduloService.listar(servicioId, activo, Roles.esAdministrador(autenticacion));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModuloDTO> crear(@Valid @RequestBody ModuloRequestDTO peticion) {
        ModuloDTO creado = moduloService.crear(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ModuloDTO actualizar(@PathVariable Long id, @Valid @RequestBody ModuloUpdateDTO peticion) {
        return moduloService.actualizar(id, peticion);
    }
}