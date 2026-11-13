package com.banco.app.controller;

import com.banco.app.config.Roles;
import com.banco.app.dto.request.ServicioRequestDTO;
import com.banco.app.dto.request.ServicioUpdateDTO;
import com.banco.app.dto.response.ServicioDTO;
import com.banco.app.service.ServicioService;
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

// Catalogo de servicios bancarios. GET lo comparten ADMIN y OPERADOR;
// las mutaciones son exclusivas de ADMIN (403 para OPERADOR).
@RestController
@RequestMapping("/api/v1/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    // Query param opcional activo=true|false: solo ADMIN puede pedir inactivos
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public List<ServicioDTO> listar(@RequestParam(name = "activo", required = false) Boolean activo,
            Authentication autenticacion) {
        return servicioService.listar(activo, Roles.esAdministrador(autenticacion));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServicioDTO> crear(@Valid @RequestBody ServicioRequestDTO peticion) {
        ServicioDTO creado = servicioService.crear(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ServicioDTO actualizar(@PathVariable Long id,
            @Valid @RequestBody ServicioUpdateDTO peticion) {
        return servicioService.actualizar(id, peticion);
    }
}