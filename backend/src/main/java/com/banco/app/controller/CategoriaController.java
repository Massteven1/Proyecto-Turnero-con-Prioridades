package com.banco.app.controller;

import com.banco.app.dto.request.CategoriaUpdateDTO;
import com.banco.app.dto.response.CategoriaDTO;
import com.banco.app.service.CategoriaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Catalogo de categorias de prioridad. GET lo comparten ADMIN y OPERADOR;
// el renombre es exclusivo de ADMIN.
@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public List<CategoriaDTO> listar() {
        return categoriaService.listar();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoriaDTO actualizar(@PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateDTO peticion) {
        return categoriaService.actualizar(id, peticion);
    }
}