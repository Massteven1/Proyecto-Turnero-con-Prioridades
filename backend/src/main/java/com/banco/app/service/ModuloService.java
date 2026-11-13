package com.banco.app.service;

import com.banco.app.config.ZonaHoraria;
import com.banco.app.dto.request.ModuloRequestDTO;
import com.banco.app.dto.request.ModuloUpdateDTO;
import com.banco.app.dto.response.ModuloDTO;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.Servicio;
import com.banco.app.repository.EstadoColaServicioRepository;
import com.banco.app.repository.ModuloAtencionRepository;
import com.banco.app.repository.ServicioRepository;
import com.banco.app.repository.TurnoActivoView;
import com.banco.app.repository.TurnoRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Catalogo de modulos o ventanillas (contrato GET/POST/PUT /api/v1/modulos).
// Reglas de la documentacion:
// - el nombre es unico globalmente ignorando mayusculas/minusculas y espacios
//   exteriores (409 Conflict)
// - el servicio padre debe existir (404) y estar activo (409)
// - si el modulo ya tiene turnos asociados, su servicioId es inmutable (409)
// - no se desactiva un modulo ocupado (LLAMADO/EN_ATENCION) ni el ultimo
//   modulo activo de un servicio que aun tiene cola EN_ESPERA hoy (409)
// - activar un modulo exige que su servicio padre este activo (409)
@Service
public class ModuloService {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS =
            List.of(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);

    private final ModuloAtencionRepository moduloAtencionRepository;
    private final ServicioRepository servicioRepository;
    private final EstadoColaServicioRepository estadoColaServicioRepository;
    private final TurnoRepository turnoRepository;

    public ModuloService(ModuloAtencionRepository moduloAtencionRepository,
            ServicioRepository servicioRepository,
            EstadoColaServicioRepository estadoColaServicioRepository,
            TurnoRepository turnoRepository) {
        this.moduloAtencionRepository = moduloAtencionRepository;
        this.servicioRepository = servicioRepository;
        this.estadoColaServicioRepository = estadoColaServicioRepository;
        this.turnoRepository = turnoRepository;
    }

    @Transactional(readOnly = true)
    public List<ModuloDTO> listar(Long servicioId, Boolean activo, boolean administrador) {
        // Un OPERADOR solo ve modulos activos y no puede pedir los inactivos
        if (!administrador && Boolean.FALSE.equals(activo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un OPERADOR solo puede consultar módulos activos");
        }
        Boolean filtro = administrador ? activo : Boolean.TRUE;

        List<ModuloAtencion> modulos = moduloAtencionRepository.buscar(servicioId, filtro);
        Map<Long, String> turnosActivos = buscarTurnosActivos(
                modulos.stream().map(ModuloAtencion::getId).toList());

        return modulos.stream()
                .map(modulo -> ModuloDTO.de(modulo, turnosActivos.get(modulo.getId())))
                .toList();
    }

    @Transactional
    public ModuloDTO crear(ModuloRequestDTO peticion) {
        String nombre = normalizarNombre(peticion.getNombre());

        // El contrato manda bloquear el servicio antes de crear el modulo y
        // revalidar su actividad dentro de la transaccion
        Servicio servicio = servicioRepository.findByIdForUpdate(peticion.getServicioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        if (!Boolean.TRUE.equals(servicio.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio está inactivo; no admite módulos activos");
        }
        validarNombreDisponible(nombre, null);

        ModuloAtencion modulo = new ModuloAtencion();
        modulo.setServicioId(servicio.getId());
        modulo.setNombre(nombre);
        // Todo modulo nuevo nace activo (contrato del endpoint)
        modulo.setActivo(true);
        ModuloAtencion guardado = moduloAtencionRepository.save(modulo);

        return ModuloDTO.de(guardado, servicio, null);
    }

    @Transactional
    public ModuloDTO actualizar(Long id, ModuloUpdateDTO peticion) {
        String nombre = normalizarNombre(peticion.getNombre());

        // Lectura previa solo para conocer el servicio actual: los bloqueos se
        // toman despues, en el orden global que exige el contrato.
        ModuloAtencion previo = moduloAtencionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo no encontrado"));

        boolean cambiaServicio = !peticion.getServicioId().equals(previo.getServicioId());
        List<Long> serviciosImplicados = cambiaServicio
                ? List.of(previo.getServicioId(), peticion.getServicioId())
                : List.of(previo.getServicioId());

        // Orden global de bloqueos: servicios implicados por id ascendente,
        // despues el modulo y al final los estados de cola por servicioId
        Map<Long, Servicio> servicios = new HashMap<>();
        servicioRepository.bloquearPorIds(serviciosImplicados)
                .forEach(servicio -> servicios.put(servicio.getId(), servicio));

        ModuloAtencion modulo = moduloAtencionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo no encontrado"));

        serviciosImplicados.stream().distinct().sorted()
                .forEach(servicioId -> estadoColaServicioRepository.bloquearPorServicioId(servicioId));

        // Si otro proceso movio el modulo entre la lectura previa y el bloqueo,
        // los servicios bloqueados no cubren su servicio real: se aborta para
        // no operar sobre un estado inconsistente.
        if (!Objects.equals(modulo.getServicioId(), previo.getServicioId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El módulo cambió de servicio durante la operación; vuelva a intentarlo");
        }

        Servicio destino = servicios.get(peticion.getServicioId());
        if (destino == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado");
        }
        if (!Boolean.TRUE.equals(destino.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El servicio destino está inactivo; no admite módulos activos");
        }
        validarNombreDisponible(nombre, id);

        if (cambiaServicio && turnoRepository.existsByModuloId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El módulo tiene turnos asociados; su servicio no puede cambiarse");
        }

        boolean seDesactiva = Boolean.TRUE.equals(modulo.getActivo())
                && Boolean.FALSE.equals(peticion.getActivo());

        if (seDesactiva) {
            // Ocupado: turno LLAMADO o EN_ATENCION de cualquier fecha
            if (turnoRepository.existsByModuloIdAndEstadoIn(id, ESTADOS_ACTIVOS)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El módulo tiene un turno activo; resuélvalo antes de desactivarlo");
            }
            // Ultimo modulo activo del servicio que pierde la ventanilla. Se mira
            // el servicio ORIGINAL: si el modulo tambien se mueve, la capacidad
            // que se reduce es la del servicio del que sale.
            Long servicioOriginal = modulo.getServicioId();
            boolean esUltimoActivo =
                    moduloAtencionRepository.countByServicioIdAndActivo(servicioOriginal, true) <= 1;
            boolean hayEsperaHoy = turnoRepository.existsByServicioIdAndFechaOperacionAndEstado(
                    servicioOriginal, LocalDate.now(ZonaHoraria.ZONA), EstadoTurno.EN_ESPERA);
            if (esUltimoActivo && hayEsperaHoy) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Es el último módulo activo del servicio y aún hay turnos en espera hoy");
            }
        }

        modulo.setServicioId(peticion.getServicioId());
        modulo.setNombre(nombre);
        // Activar exige servicio padre activo: ya quedo validado arriba
        modulo.setActivo(peticion.getActivo());
        moduloAtencionRepository.save(modulo);

        return ModuloDTO.de(modulo, destino, buscarTurnoActivo(id));
    }

    private void validarNombreDisponible(String nombre, Long idExcluido) {
        if (moduloAtencionRepository.contarNombreNormalizado(nombre, idExcluido) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un módulo con ese nombre");
        }
    }

    private Map<Long, String> buscarTurnosActivos(List<Long> moduloIds) {
        if (moduloIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> turnosActivos = new HashMap<>();
        for (TurnoActivoView turno : turnoRepository.buscarTurnosActivos(moduloIds, ESTADOS_ACTIVOS)) {
            turnosActivos.put(turno.getModuloId(), turno.getCodigo());
        }
        return turnosActivos;
    }

    private String buscarTurnoActivo(Long moduloId) {
        return buscarTurnosActivos(List.of(moduloId)).get(moduloId);
    }

    // Nombre obligatorio de 3 a 50 caracteres, sin espacios exteriores
    private String normalizarNombre(String nombre) {
        String recortado = nombre.trim();
        if (recortado.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 3 caracteres");
        }
        return recortado;
    }
}