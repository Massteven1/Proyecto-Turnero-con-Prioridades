package com.banco.app.controller;

import com.banco.app.config.Roles;
import com.banco.app.dto.request.CambioEstadoDTO;
import com.banco.app.dto.request.ModoConsulta;
import com.banco.app.dto.request.RegistroTurnoDTO;
import com.banco.app.dto.response.IndicadoresDTO;
import com.banco.app.dto.response.TurnoColaDTO;
import com.banco.app.dto.response.TurnoDTO;
import com.banco.app.dto.response.TurnoDetalleDTO;
import com.banco.app.dto.response.TurnoResumenDTO;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.security.Identidad;
import com.banco.app.service.TurnoConsultaService;
import com.banco.app.service.TurnoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Emision, consulta y transiciones de turnos (contrato de las filas 19, 21, 24,
// 25 y 26). El actor se extrae del JWT: nunca se acepta un operadorId enviado por
// el cliente.
@RestController
@RequestMapping("/api/v1/turnos")
public class TurnoController {

    private final TurnoService turnoService;
    private final TurnoConsultaService consultaService;

    public TurnoController(TurnoService turnoService, TurnoConsultaService consultaService) {
        this.turnoService = turnoService;
        this.consultaService = consultaService;
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

    // GET /api/v1/turnos: una sola ruta para dos modos. ?modo= ausente equivale a
    // COLA; cada modo devuelve una lista distinta, por eso el controlador
    // despacha al metodo tipado que corresponde.
    // - COLA (ADMIN y OPERADOR): solo EN_ESPERA de hoy; servicioId obligatorio y
    //   sin fecha ni estado.
    // - HISTORIAL (exclusivo ADMIN): filtros opcionales fecha, servicioId y estado.
    // El tipo de retorno es ResponseEntity<?> porque los dos modos no comparten
    // DTO; @ArraySchema documenta en Swagger esa union que el compilador no ve.
    @Operation(summary = "Consultar la cola operativa o el historial de turnos",
            description = "modo=COLA (valor por defecto): turnos EN_ESPERA de la fecha actual, "
                    + "servicioId obligatorio, sin fecha ni estado. modo=HISTORIAL: exclusivo de "
                    + "ADMIN, con filtros opcionales fecha, servicioId y estado.")
    @ApiResponse(responseCode = "200", description = "List<TurnoColaDTO> en COLA; "
            + "List<TurnoResumenDTO> en HISTORIAL",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(type = "object", oneOf = {
                            TurnoColaDTO.class, TurnoResumenDTO.class }))))
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public ResponseEntity<?> consultar(@RequestParam(name = "modo", required = false) String modo,
            @RequestParam(name = "servicioId", required = false) Long servicioId,
            @RequestParam(name = "fecha", required = false) LocalDate fecha,
            @RequestParam(name = "estado", required = false) EstadoTurno estado,
            Authentication autenticacion) {
        if (ModoConsulta.desde(modo) == ModoConsulta.HISTORIAL) {
            return ResponseEntity.ok(consultaService.historial(fecha, servicioId, estado,
                    Roles.esAdministrador(autenticacion)));
        }
        return ResponseEntity.ok(consultaService.cola(servicioId, fecha, estado));
    }

    // GET /api/v1/turnos/{id}: ADMIN consulta cualquier fecha y OPERADOR solo los
    // turnos del dia actual (403 si el turno es historico).
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')")
    public TurnoDetalleDTO detalle(@PathVariable Long id, Authentication autenticacion) {
        return consultaService.detalle(id, Roles.esAdministrador(autenticacion));
    }

    // GET /api/v1/turnos/indicadores: exclusivo ADMIN. El literal gana sobre
    // /{id} en el patron de rutas, por eso "indicadores" no cae en el detalle.
    @GetMapping("/indicadores")
    @PreAuthorize("hasRole('ADMIN')")
    public IndicadoresDTO indicadores(@RequestParam(name = "fecha", required = false) LocalDate fecha,
            @RequestParam(name = "servicioId", required = false) Long servicioId) {
        return consultaService.indicadores(fecha, servicioId);
    }
}