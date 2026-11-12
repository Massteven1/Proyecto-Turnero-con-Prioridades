package com.banco.app.controller;

import com.banco.app.dto.request.UsuarioCreateDTO;
import com.banco.app.dto.request.UsuarioUpdateDTO;
import com.banco.app.dto.response.UsuarioDTO;
import com.banco.app.service.UsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// CRUD de usuarios del personal bancario. Todo el controlador exige ADMIN
// (@PreAuthorize del contrato; requiere @EnableMethodSecurity en SecurityConfig).
// Un OPERADOR autenticado recibe 403 con el ErrorResponseDTO del contrato.
@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Query param opcional activo=true|false para filtrar la lista
    @GetMapping
    public List<UsuarioDTO> listar(@RequestParam(name = "activo", required = false) Boolean activo) {
        return usuarioService.listar(activo);
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(@Valid @RequestBody UsuarioCreateDTO peticion) {
        UsuarioDTO creado = usuarioService.crear(peticion);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public UsuarioDTO actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioUpdateDTO peticion) {
        return usuarioService.actualizar(id, peticion);
    }
}
