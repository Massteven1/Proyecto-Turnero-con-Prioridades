package com.banco.app.service;

import com.banco.app.config.ZonaHoraria;
import com.banco.app.dto.request.CambioEstadoDTO;
import com.banco.app.dto.request.RegistroTurnoDTO;
import com.banco.app.dto.response.SiguienteTurnoResponseDTO;
import com.banco.app.dto.response.TurnoDTO;
import com.banco.app.entity.Categoria;
import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.HistorialTurno;
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
import jakarta.persistence.PersistenceContext;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Reglas de emision, llamado y transiciones de turnos (contrato de filas 19-21).
// Puntos centrales de la documentacion:
// - El actor (operador que emite, llama o cambia el estado) sale del JWT.
// - Todo bloqueo respeta el orden global: cuentas implicadas por id ascendente,
//   servicio, categoria (solo emision), modulo, estado de cola y turno objetivo.
// - La marca de tiempo se captura UNA sola vez, ya con los bloqueos tomados.
// - El consecutivo diario y el contador de preferentes viven en
//   estado_cola_servicio y se reinician con la primera operacion del dia.
// - Seleccion: conjunto preferente efectivo (preferente + Regular envejecido)
//   contra Regular no envejecido, con FIFO y desempate por id.
@Service
public class TurnoService {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS =
            List.of(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);

    // Destinos que acepta PATCH /turnos/{id}/estado: LLAMADO y EN_ESPERA no se
    // alcanzan por este endpoint.
    private static final Set<EstadoTurno> DESTINOS_PERMITIDOS =
            Set.of(EstadoTurno.EN_ATENCION, EstadoTurno.FINALIZADO,
                    EstadoTurno.CANCELADO, EstadoTurno.AUSENTE);

    private static final String MOTIVO_EMISION = "Emisión inicial del turno";

    private final TurnoRepository turnoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ServicioRepository servicioRepository;
    private final CategoriaRepository categoriaRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final EstadoColaServicioRepository estadoColaServicioRepository;
    private final HistorialTurnoRepository historialTurnoRepository;

    // Se usa para desvincular la lectura previa del turno objetivo antes de
    // releerlo con el SQL nativo FOR UPDATE: en REPEATABLE_READ una lectura
    // normal devuelve la instantanea previa a la espera del bloqueo y el
    // contrato exige revalidar con el dato vivo.
    @PersistenceContext
    private EntityManager entityManager;

    public TurnoService(TurnoRepository turnoRepository,
            UsuarioRepository usuarioRepository,
            ServicioRepository servicioRepository,
            CategoriaRepository categoriaRepository,
            ModuloAtencionRepository moduloAtencionRepository,
            EstadoColaServicioRepository estadoColaServicioRepository,
            HistorialTurnoRepository historialTurnoRepository) {
        this.turnoRepository = turnoRepository;
        this.usuarioRepository = usuarioRepository;
        this.servicioRepository = servicioRepository;
        this.categoriaRepository = categoriaRepository;
        this.moduloAtencionRepository = moduloAtencionRepository;
        this.estadoColaServicioRepository = estadoColaServicioRepository;
        this.historialTurnoRepository = historialTurnoRepository;
    }

    // POST /api/v1/turnos
    @Transactional
    public TurnoDTO registrar(RegistroTurnoDTO peticion, Long actorId, boolean administrador) {
        String identificador = normalizarIdentificador(peticion.getIdentificadorCliente());

        // Orden global de bloqueos: cuenta autenticada, servicio, categoria y
        // estado de cola del servicio.
        Usuario actor = usuarioRepository.findByIdForUpdate(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La cuenta no existe o está inactiva"));
        validarCuentaVigente(actor, administrador);

        Servicio servicio = servicioRepository.findByIdForUpdate(peticion.getServicioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        Categoria categoria = categoriaRepository.findByIdForUpdate(peticion.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));
        EstadoColaServicio estado = estadoColaServicioRepository.bloquearPorServicioId(servicio.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El servicio no tiene fila de estado de cola"));

        // Una sola marca temporal para toda la transaccion, ya con los bloqueos
        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.ZONA);
        LocalDate hoy = ahora.toLocalDate();

        // Revalidacion dentro de la transaccion: el catalogo pudo cambiar
        // mientras la peticion esperaba los bloqueos.
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio está inactivo; no admite nuevos turnos");
        }
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La categoría está inactiva; no admite nuevos turnos");
        }

        reiniciarDiaSiCorresponde(estado, hoy);
        int consecutivo = estado.getUltimoConsecutivo() + 1;

        Turno turno = new Turno();
        turno.setCodigo(construirCodigo(servicio.getCodigoPrefijo(), hoy, consecutivo));
        turno.setServicioId(servicio.getId());
        turno.setCategoriaId(categoria.getId());
        // Instantanea historica: si luego se renombra la categoria, el turno
        // conserva la denominacion vigente al emitirse.
        turno.setCategoriaNombreSnapshot(categoria.getNombre());
        turno.setFechaOperacion(hoy);
        turno.setConsecutivo(consecutivo);
        turno.setIdentificadorCliente(identificador);
        turno.setEstado(EstadoTurno.EN_ESPERA);
        turno.setFechaIngreso(ahora);

        Turno guardado = turnoRepository.save(turno);

        estado.setUltimoConsecutivo(consecutivo);
        estadoColaServicioRepository.save(estado);
        registrarHistorial(guardado.getId(), actorId, null, EstadoTurno.EN_ESPERA, MOTIVO_EMISION, ahora);

        return TurnoDTO.de(guardado, servicio, categoria, null, null,
                estado.getUmbralEnvejecimientoMin(), ahora);
    }

    // POST /api/v1/modulos/{id}/siguiente
    @Transactional
    public SiguienteTurnoResponseDTO llamarSiguiente(Long moduloId, Long actorId, boolean administrador) {
        // Lectura previa sin bloqueo y como escalar: solo hace falta el
        // servicio del modulo para fijar el orden global de bloqueos.
        Long servicioIdModulo = moduloAtencionRepository.buscarServicioId(moduloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo no encontrado"));

        Usuario actor = usuarioRepository.findByIdForUpdate(actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La cuenta no existe o está inactiva"));
        validarCuentaVigente(actor, administrador);

        Servicio servicio = servicioRepository.findByIdForUpdate(servicioIdModulo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        ModuloAtencion modulo = moduloAtencionRepository.findByIdForUpdate(moduloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo no encontrado"));
        EstadoColaServicio estado = estadoColaServicioRepository.bloquearPorServicioId(servicio.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El servicio no tiene fila de estado de cola"));

        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.ZONA);
        LocalDate hoy = ahora.toLocalDate();
        reiniciarDiaSiCorresponde(estado, hoy);

        // Si otro proceso movio el modulo entre la lectura y el bloqueo, el
        // servicio bloqueado no es el suyo: se aborta para no operar sobre un
        // estado inconsistente.
        if (!Objects.equals(modulo.getServicioId(), servicioIdModulo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El módulo cambió de servicio durante la operación; vuelva a intentarlo");
        }
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio está inactivo; no admite llamados");
        }
        if (!Boolean.TRUE.equals(modulo.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El módulo está inactivo; no admite llamados");
        }

        // Doble exclusividad activa: la cuenta y el modulo no pueden tener ya
        // un turno LLAMADO o EN_ATENCION (de cualquier fecha).
        if (turnoRepository.existsByOperadorIdAndEstadoIn(actorId, ESTADOS_ACTIVOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cuenta ya tiene un turno activo; resuélvalo antes de llamar otro");
        }
        if (turnoRepository.existsByModuloIdAndEstadoIn(moduloId, ESTADOS_ACTIVOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El módulo ya tiene un turno activo; resuélvalo antes de llamar otro");
        }

        List<Turno> cola = turnoRepository
                .findByServicioIdAndFechaOperacionAndEstadoOrderByFechaIngresoAscIdAsc(
                        servicio.getId(), hoy, EstadoTurno.EN_ESPERA);
        if (cola.isEmpty()) {
            return SiguienteTurnoResponseDTO.colaVacia(estado.getContadorPreferentes());
        }

        Map<Long, Categoria> categorias = new HashMap<>();
        categoriaRepository.findAll().forEach(categoria -> categorias.put(categoria.getId(), categoria));

        int umbral = estado.getUmbralEnvejecimientoMin();
        int proporcion = estado.getProporcionPreferente();
        int contador = estado.getContadorPreferentes();

        List<Turno> preferentes = new ArrayList<>();
        List<Turno> regulares = new ArrayList<>();
        for (Turno candidato : cola) {
            if (esPreferenteEfectivo(candidato, categorias, umbral, ahora)) {
                preferentes.add(candidato);
            } else {
                regulares.add(candidato);
            }
        }

        Turno elegido;
        if (!preferentes.isEmpty() && !regulares.isEmpty()) {
            if (contador < proporcion) {
                elegido = preferentes.get(0);
                contador = Math.min(contador + 1, proporcion);
            } else {
                elegido = regulares.get(0);
                contador = 0;
            }
        } else if (!preferentes.isEmpty()) {
            elegido = preferentes.get(0);
            contador = Math.min(contador + 1, proporcion);
        } else {
            elegido = regulares.get(0);
            contador = 0;
        }

        // Bloqueo y revalidacion del candidato: otro operador pudo llamarlo
        // mientras esta transaccion esperaba los bloqueos anteriores. La
        // instancia que trajo la cola se desvincula antes del SQL nativo; si
        // quedara administrada, el bloqueo se tomaria pero Hibernate devolveria
        // su estado previo a la espera.
        entityManager.detach(elegido);
        Turno turno = turnoRepository.findByIdForUpdate(elegido.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "El turno seleccionado ya no está disponible"));
        if (turno.getEstado() != EstadoTurno.EN_ESPERA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno seleccionado ya no está disponible");
        }

        turno.setEstado(EstadoTurno.LLAMADO);
        turno.setModuloId(moduloId);
        turno.setOperadorId(actorId);
        turno.setFechaLlamado(ahora);
        turnoRepository.save(turno);

        estado.setContadorPreferentes(contador);
        estadoColaServicioRepository.save(estado);

        registrarHistorial(turno.getId(), actorId, EstadoTurno.EN_ESPERA, EstadoTurno.LLAMADO,
                "Llamado en " + modulo.getNombre(), ahora);

        TurnoDTO dto = TurnoDTO.de(turno, servicio, categorias.get(turno.getCategoriaId()),
                modulo, actor, umbral, ahora);
        return SiguienteTurnoResponseDTO.llamado(
                "Turno " + turno.getCodigo() + " llamado en " + modulo.getNombre() + ".", dto, contador);
    }

    // PATCH /api/v1/turnos/{id}/estado
    @Transactional
    public TurnoDTO cambiarEstado(Long turnoId, CambioEstadoDTO peticion, Long actorId, boolean administrador) {
        EstadoTurno destino = peticion.getNuevoEstado();
        if (!DESTINOS_PERMITIDOS.contains(destino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El estado solicitado no es válido; use EN_ATENCION, FINALIZADO, CANCELADO o AUSENTE");
        }
        // Validacion de entrada antes de tocar la base: el motivo es obligatorio
        // (5 a 255 caracteres) solo para las salidas CANCELADO y AUSENTE.
        String motivo = normalizarMotivo(peticion.getMotivo(), destino);

        // Lectura previa sin bloqueo: define las cuentas implicadas y el orden
        // global de bloqueos.
        Turno previo = turnoRepository.findById(turnoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));

        Set<Long> cuentasImplicadas = new LinkedHashSet<>();
        cuentasImplicadas.add(actorId);
        if (previo.getOperadorId() != null) {
            cuentasImplicadas.add(previo.getOperadorId());
        }
        Map<Long, Usuario> usuarios = new HashMap<>();
        usuarioRepository.bloquearPorIds(cuentasImplicadas)
                .forEach(cuenta -> usuarios.put(cuenta.getId(), cuenta));

        Usuario actor = usuarios.get(actorId);
        if (actor == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "La cuenta no existe o está inactiva");
        }
        validarCuentaVigente(actor, administrador);

        Servicio servicio = servicioRepository.findByIdForUpdate(previo.getServicioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));

        ModuloAtencion modulo = null;
        if (previo.getModuloId() != null) {
            modulo = moduloAtencionRepository.findByIdForUpdate(previo.getModuloId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo no encontrado"));
        }

        EstadoColaServicio estado = estadoColaServicioRepository.bloquearPorServicioId(servicio.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El servicio no tiene fila de estado de cola"));

        // Se conservan los datos con los que se eligieron las cuentas implicadas y
        // se desvincula la lectura previa antes de releer el turno con FOR UPDATE
        // (ver TurnoRepository.findByIdForUpdate).
        EstadoTurno estadoPrevio = previo.getEstado();
        Long operadorPrevio = previo.getOperadorId();
        LocalDate fechaPrevia = previo.getFechaOperacion();
        entityManager.detach(previo);
        Turno turno = turnoRepository.findByIdForUpdate(turnoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turno no encontrado"));

        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.ZONA);
        LocalDate hoy = ahora.toLocalDate();

        // Revalidacion tras los bloqueos: si el turno cambio entre la lectura
        // previa y el bloqueo, las cuentas bloqueadas pueden no ser las suyas.
        if (turno.getEstado() != estadoPrevio
                || !Objects.equals(turno.getOperadorId(), operadorPrevio)
                || !Objects.equals(turno.getFechaOperacion(), fechaPrevia)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El turno cambió durante la operación; vuelva a intentarlo");
        }

        validarTransicion(turno, destino, actorId, administrador, hoy);

        turno.setEstado(destino);
        switch (destino) {
            case EN_ATENCION -> turno.setFechaInicioAtencion(ahora);
            case FINALIZADO -> turno.setFechaFinAtencion(ahora);
            // CANCELADO y AUSENTE comparten columna y exigen motivo
            default -> {
                turno.setFechaCancelacionAusencia(ahora);
                turno.setMotivoCancelacionAusencia(motivo);
            }
        }
        Turno guardado = turnoRepository.save(turno);

        registrarHistorial(guardado.getId(), actorId, estadoPrevio, destino, motivo, ahora);

        Usuario operadorAsignado = turno.getOperadorId() != null ? usuarios.get(turno.getOperadorId()) : null;
        Categoria categoria = categoriaRepository.findById(turno.getCategoriaId()).orElse(null);
        return TurnoDTO.de(guardado, servicio, categoria, modulo, operadorAsignado,
                estado.getUmbralEnvejecimientoMin(), ahora);
    }

    // Reglas de transicion y propiedad del turno. Se ejecutan con los bloqueos
    // tomados y devuelven 409 para transiciones invalidas y 403 para un
    // OPERADOR que intenta operar sobre un turno ajeno o de otra fecha.
    private void validarTransicion(Turno turno, EstadoTurno destino, Long actorId,
            boolean administrador, LocalDate hoy) {
        EstadoTurno actual = turno.getEstado();

        boolean transicionValida = switch (actual) {
            case EN_ESPERA -> destino == EstadoTurno.CANCELADO;
            case LLAMADO -> destino == EstadoTurno.EN_ATENCION
                    || destino == EstadoTurno.CANCELADO
                    || destino == EstadoTurno.AUSENTE;
            case EN_ATENCION -> destino == EstadoTurno.FINALIZADO;
            default -> false;
        };
        if (!transicionValida) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Transición no permitida de " + actual + " a " + destino);
        }

        if (administrador) {
            return;
        }

        if (actual == EstadoTurno.EN_ESPERA) {
            // Un OPERADOR solo cancela EN_ESPERA de la fecha de operacion actual
            if (!turno.getFechaOperacion().equals(hoy)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Un OPERADOR solo puede cancelar turnos EN_ESPERA de la fecha actual");
            }
            return;
        }

        // LLAMADO o EN_ATENCION: el OPERADOR debe ser el operador asignado
        if (!Objects.equals(turno.getOperadorId(), actorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El turno está asignado a otro operador");
        }
    }

    // Cuenta vigente dentro de la transaccion: activa y con el mismo rol del JWT.
    private void validarCuentaVigente(Usuario cuenta, boolean administrador) {
        if (!Boolean.TRUE.equals(cuenta.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "La cuenta no existe o está inactiva");
        }
        RolUsuario esperado = administrador ? RolUsuario.ADMIN : RolUsuario.OPERADOR;
        if (cuenta.getRol() != esperado) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "El rol de la cuenta cambió; vuelva a iniciar sesión");
        }
    }

    // Primera operacion del dia sobre el servicio: reinicia consecutivo y
    // contador. Los turnos de dias anteriores no se tocan.
    private void reiniciarDiaSiCorresponde(EstadoColaServicio estado, LocalDate hoy) {
        if (estado.getFechaOperacion().isBefore(hoy)) {
            estado.setFechaOperacion(hoy);
            estado.setUltimoConsecutivo(0);
            estado.setContadorPreferentes(0);
        }
    }

    private boolean esPreferenteEfectivo(Turno turno, Map<Long, Categoria> categorias,
            int umbralEnvejecimientoMin, LocalDateTime ahora) {
        Categoria categoria = categorias.get(turno.getCategoriaId());
        if (categoria != null && Boolean.TRUE.equals(categoria.getPreferente())) {
            return true;
        }
        long esperaMinutos = Duration.between(turno.getFechaIngreso(), ahora).toMinutes();
        return esperaMinutos >= umbralEnvejecimientoMin;
    }

    private void registrarHistorial(Long turnoId, Long actorId, EstadoTurno anterior,
            EstadoTurno nuevo, String motivo, LocalDateTime ahora) {
        HistorialTurno historial = new HistorialTurno();
        historial.setTurnoId(turnoId);
        historial.setOperadorId(actorId);
        historial.setEstadoAnterior(anterior);
        historial.setEstadoNuevo(nuevo);
        historial.setFechaCambio(ahora);
        historial.setMotivo(motivo);
        historialTurnoRepository.save(historial);
    }

    // PREFIJO-YYYYMMDD-NNN: minimo tres digitos; si el consecutivo supera 999,
    // String.format conserva los digitos de mas (1000, 1001, ...).
    private String construirCodigo(String prefijo, LocalDate fecha, int consecutivo) {
        return prefijo + "-" + fecha.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format("%03d", consecutivo);
    }

    // Identificador de 3 a 100 caracteres sin espacios exteriores (el CHECK de
    // la tabla tambien recorta antes de medir).
    private String normalizarIdentificador(String identificador) {
        String recortado = identificador.trim();
        if (recortado.length() < 3 || recortado.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El identificador del cliente debe tener entre 3 y 100 caracteres");
        }
        return recortado;
    }

    // Motivo obligatorio de 5 a 255 caracteres en CANCELADO y AUSENTE; en el
    // resto de transiciones es opcional y el texto en blanco se guarda como null.
    private String normalizarMotivo(String motivo, EstadoTurno destino) {
        boolean obligatorio = destino == EstadoTurno.CANCELADO || destino == EstadoTurno.AUSENTE;
        String recortado = motivo == null ? null : motivo.trim();

        if (obligatorio) {
            if (recortado == null || recortado.length() < 5 || recortado.length() > 255) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El motivo es obligatorio y debe tener entre 5 y 255 caracteres");
            }
            return recortado;
        }
        if (recortado == null || recortado.isEmpty()) {
            return null;
        }
        if (recortado.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El motivo no debe exceder 255 caracteres");
        }
        return recortado;
    }
}