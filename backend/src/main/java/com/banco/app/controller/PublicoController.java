package com.banco.app.controller;

import com.banco.app.dto.response.PantallaPublicaDTO;
import com.banco.app.service.TurnoConsultaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Pantalla de sala de espera: ruta publica sin JWT (permitAll en SecurityConfig,
// contrato de la fila 22). Solo lectura y sin datos personales.
@RestController
@RequestMapping("/api/v1/publico")
public class PublicoController {

    private final TurnoConsultaService consultaService;

    public PublicoController(TurnoConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    @GetMapping("/pantalla")
    public PantallaPublicaDTO pantalla() {
        return consultaService.pantalla();
    }
}
