package com.banco.app.controller;

import com.banco.app.dto.request.ParametrosColaDTO;
import com.banco.app.dto.response.EstadoColaDTO;
import com.banco.app.service.ParametrosColaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Parametros de cola por servicio. GET lo comparten ADMIN y OPERADOR;
// la actualizacion es exclusiva de ADMIN.
@RestController
@RequestMapping("/api/v1/parametros-cola")
public class ParametrosColaController {

    private final ParametrosColaService parametrosColaService;

    public ParametrosColaController(ParametrosColaService parametrosColaService) {
        this.parametrosColaService = parametrosColaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public List<EstadoColaDTO> listar() {
        return parametrosColaService.listar();
    }

    @PutMapping("/{servicioId}")
    @PreAuthorize("hasRole('ADMIN')")
    public EstadoColaDTO actualizar(@PathVariable Long servicioId,
            @Valid @RequestBody ParametrosColaDTO peticion) {
        return parametrosColaService.actualizar(servicioId, peticion);
    }
}