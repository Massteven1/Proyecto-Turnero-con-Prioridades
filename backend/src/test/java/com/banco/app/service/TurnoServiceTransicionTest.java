package com.banco.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.banco.app.dto.request.CambioEstadoDTO;
import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Servicio;
import com.banco.app.entity.Turno;
import com.banco.app.entity.Usuario;
import com.banco.app.repository.CategoriaRepository;
import com.banco.app.repository.EstadoColaServicioRepository;
import com.banco.app.repository.HistorialTurnoRepository;
import com.banco.app.repository.ModuloAtencionRepository;
import com.banco.app.repository.ServicioRepository;
import com.banco.app.repository.TurnoRepository;
import com.banco.app.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

// Reglas de transicion y propiedad del turno. Se ejercita el punto de entrada
// real (cambiarEstado) con los repositorios simulados; los casos probados
// terminan en 400/403/409, es decir antes de escribir en la base.
class TurnoServiceTransicionTest {

    private static final long ACTOR_ID = 7L;
    private static final long OTRO_OPERADOR_ID = 99L;
    private static final long TURNO_ID = 1L;

    private TurnoRepository turnoRepository;
    private UsuarioRepository usuarioRepository;
    private ServicioRepository servicioRepository;
    private CategoriaRepository categoriaRepository;
    private ModuloAtencionRepository moduloAtencionRepository;
    private EstadoColaServicioRepository estadoColaServicioRepository;
    private HistorialTurnoRepository historialTurnoRepository;
    private TurnoService turnoService;

    @BeforeEach
    void preparar() {
        turnoRepository = mock(TurnoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        servicioRepository = mock(ServicioRepository.class);
        categoriaRepository = mock(CategoriaRepository.class);
        moduloAtencionRepository = mock(ModuloAtencionRepository.class);
        estadoColaServicioRepository = mock(EstadoColaServicioRepository.class);
        historialTurnoRepository = mock(HistorialTurnoRepository.class);

        turnoService = new TurnoService(turnoRepository, usuarioRepository, servicioRepository,
                categoriaRepository, moduloAtencionRepository, estadoColaServicioRepository,
                historialTurnoRepository);
        ReflectionTestUtils.setField(turnoService, "entityManager", mock(EntityManager.class));
    }

    @Test
    @DisplayName("un turno FINALIZADO no se puede finalizar dos veces (reto de devoluciones duplicadas)")
    void finalizadoAFinalizadoDevuelve409() {
        Turno turno = turnoCon(EstadoTurno.FINALIZADO, ACTOR_ID, LocalDate.now());
        simularDatos(turno, RolUsuario.OPERADOR, true);

        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.FINALIZADO, null, false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.getReason()).isEqualTo("Transición no permitida de FINALIZADO a FINALIZADO");
    }

    @Test
    @DisplayName("EN_ESPERA no salta directo a EN_ATENCION")
    void enEsperaAEnAtencionDevuelve409() {
        Turno turno = turnoCon(EstadoTurno.EN_ESPERA, null, LocalDate.now());
        simularDatos(turno, RolUsuario.OPERADOR, true);

        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.EN_ATENCION, null, false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.getReason()).isEqualTo("Transición no permitida de EN_ESPERA a EN_ATENCION");
    }

    @Test
    @DisplayName("LLAMADO no salta directo a FINALIZADO")
    void llamadoAFinalizadoDevuelve409() {
        Turno turno = turnoCon(EstadoTurno.LLAMADO, ACTOR_ID, LocalDate.now());
        simularDatos(turno, RolUsuario.OPERADOR, true);

        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.FINALIZADO, null, false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("un OPERADOR no puede operar el turno de otro operador")
    void operadorNoPuedeOperarTurnoAjeno() {
        Turno turno = turnoCon(EstadoTurno.LLAMADO, OTRO_OPERADOR_ID, LocalDate.now());
        simularDatos(turno, RolUsuario.OPERADOR, true);

        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.EN_ATENCION, null, false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.getReason()).isEqualTo("El turno está asignado a otro operador");
    }

    @Test
    @DisplayName("un ADMIN no queda limitado por la propiedad del turno")
    void administradorNoQuedaLimitadoPorLaPropiedad() {
        Turno turno = turnoCon(EstadoTurno.LLAMADO, OTRO_OPERADOR_ID, LocalDate.now());
        simularDatos(turno, RolUsuario.ADMIN, true);

        // Sin propiedad de por medio, el rechazo es por la transicion (409), no por 403.
        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.FINALIZADO, null, true);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("un OPERADOR solo cancela turnos EN_ESPERA de la fecha actual")
    void operadorNoCancelaEsperaDeOtroDia() {
        Turno turno = turnoCon(EstadoTurno.EN_ESPERA, null, LocalDate.now().minusDays(1));
        simularDatos(turno, RolUsuario.OPERADOR, true);

        ResponseStatusException error = cambiarEstado(turno, EstadoTurno.CANCELADO, "Cliente se retiro", false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.getReason())
                .isEqualTo("Un OPERADOR solo puede cancelar turnos EN_ESPERA de la fecha actual");
    }

    @Test
    @DisplayName("cancelar sin motivo se rechaza con 400 antes de tocar la base")
    void cancelarSinMotivoDevuelve400() {
        ResponseStatusException error = cambiarEstado(null, EstadoTurno.CANCELADO, null, true);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getReason())
                .isEqualTo("El motivo es obligatorio y debe tener entre 5 y 255 caracteres");
        verifyNoInteractions(turnoRepository);
    }

    @Test
    @DisplayName("un motivo demasiado corto tambien se rechaza con 400")
    void motivoDemasiadoCortoDevuelve400() {
        ResponseStatusException error = cambiarEstado(null, EstadoTurno.AUSENTE, "abc", false);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(turnoRepository);
    }

    @Test
    @DisplayName("un destino fuera del catalogo del PATCH se rechaza con 400")
    void destinoNoPermitidoDevuelve400() {
        ResponseStatusException error = cambiarEstado(null, EstadoTurno.EN_ESPERA, null, true);

        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(error.getReason()).contains("El estado solicitado no es válido");
        verifyNoInteractions(turnoRepository);
    }

    // ---- utilidades ----

    private ResponseStatusException cambiarEstado(Turno turno, EstadoTurno destino, String motivo,
            boolean administrador) {
        CambioEstadoDTO peticion = new CambioEstadoDTO();
        peticion.setNuevoEstado(destino);
        peticion.setMotivo(motivo);

        return catchThrowableOfType(
                () -> turnoService.cambiarEstado(TURNO_ID, peticion, ACTOR_ID, administrador),
                ResponseStatusException.class);
    }

    private Turno turnoCon(EstadoTurno estado, Long operadorId, LocalDate fechaOperacion) {
        Turno turno = new Turno();
        turno.setId(TURNO_ID);
        turno.setCodigo("CAJ-20261116-001");
        turno.setServicioId(1L);
        turno.setCategoriaId(1L);
        turno.setModuloId(1L);
        turno.setEstado(estado);
        turno.setOperadorId(operadorId);
        turno.setFechaOperacion(fechaOperacion);
        return turno;
    }

    // Deja listos los bloqueos que cambiarEstado toma antes de validar.
    private void simularDatos(Turno turno, RolUsuario rolActor, boolean activo) {
        Usuario actor = new Usuario();
        actor.setId(ACTOR_ID);
        actor.setUsername("operador");
        actor.setRol(rolActor);
        actor.setActivo(activo);

        Servicio servicio = new Servicio();
        servicio.setId(1L);
        servicio.setNombre("Caja");
        servicio.setActivo(true);

        ModuloAtencion modulo = new ModuloAtencion();
        modulo.setId(1L);
        modulo.setServicioId(1L);
        modulo.setNombre("Caja 1");
        modulo.setActivo(true);

        EstadoColaServicio estadoCola = new EstadoColaServicio();
        estadoCola.setId(1L);
        estadoCola.setServicioId(1L);
        estadoCola.setFechaOperacion(LocalDate.now());
        estadoCola.setUmbralEnvejecimientoMin(60);
        estadoCola.setContadorPreferentes(0);
        estadoCola.setProporcionPreferente(3);

        when(turnoRepository.findById(turno.getId())).thenReturn(Optional.of(turno));
        when(usuarioRepository.bloquearPorIds(any())).thenReturn(List.of(actor));
        when(servicioRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(servicio));
        when(moduloAtencionRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(modulo));
        when(estadoColaServicioRepository.bloquearPorServicioId(anyLong())).thenReturn(Optional.of(estadoCola));
        when(turnoRepository.findByIdForUpdate(turno.getId())).thenReturn(Optional.of(turno));
    }
}
