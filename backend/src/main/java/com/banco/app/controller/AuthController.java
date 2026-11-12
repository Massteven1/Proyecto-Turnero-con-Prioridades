package com.banco.app.controller;

import com.banco.app.dto.request.LoginRequestDTO;
import com.banco.app.dto.response.AuthResponseDTO;
import com.banco.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Ruta publica (permitAll en SecurityConfig): recibe credenciales y devuelve
// el JWT firmado con la identidad del usuario para usarlo en el header
// Authorization: Bearer <token> de las demas peticiones.
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO peticion) {
        return ResponseEntity.ok(authService.login(peticion));
    }
}
