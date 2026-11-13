package com.banco.app.service;

import com.banco.app.config.ZonaHoraria;
import com.banco.app.dto.request.ServicioRequestDTO;
import com.banco.app.dto.request.ServicioUpdateDTO;
import com.banco.app.dto.response.ServicioDTO;
import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.Servicio;
import com.banco.app.repository.EstadoColaServicioRepository;
import com.banco.app.repository.ModuloAtencionRepository;
import com.banco.app.repository.ServicioRepository;
import com.banco.app.repository.TurnoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Catalogo de servicios bancarios (contrato GET/POST/PUT /api/v1/servicios).
// Reglas de la documentacion:
// - codigoPrefijo es inmutable y solo admite mayusculas de 2 a 10 letras
// - al crear se inserta en la misma transaccion su fila 1:1 de estado de cola
//   con los valores por defecto (15 minutos y proporcion 3:1)
// - la baja es logica y se rechaza con 409 si hay cola EN_ESPERA del dia o
//   turnos LLAMADO/EN_ATENCION de cualquier fecha
// - desactivar el servicio desactiva en cascada sus modulos; reactivarlo los
//   deja inactivos para que el ADMIN los active uno por uno
@Service
public class ServicioService {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS =
            List.of(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);

    private static final int UMBRAL_ENVEJECIMIENTO_DEFECTO = 15;
    private static final int PROPORCION_PREFERENTE_DEFECTO = 3;

    private final ServicioRepository servicioRepository;
    private final ModuloAtencionRepository moduloAtencionRepository;
    private final EstadoColaServicioRepository estadoColaServicioRepository;
    private final TurnoRepository turnoRepository;

    public ServicioService(ServicioRepository servicioRepository,
            ModuloAtencionRepository moduloAtencionRepository,
            EstadoColaServicioRepository estadoColaServicioRepository,
            TurnoRepository turnoRepository) {
        this.servicioRepository = servicioRepository;
        this.moduloAtencionRepository = moduloAtencionRepository;
        this.estadoColaServicioRepository = estadoColaServicioRepository;
        this.turnoRepository = turnoRepository;
    }

    @Transactional(readOnly = true)
    public List<ServicioDTO> listar(Boolean activo, boolean administrador) {
        // Un OPERADOR solo ve servicios activos y no puede pedir los inactivos
        if (!administrador && Boolean.FALSE.equals(activo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Un OPERADOR solo puede consultar servicios activos");
        }
        Boolean filtro = administrador ? activo : Boolean.TRUE;

        List<Servicio> servicios = filtro == null
                ? servicioRepository.findAllByOrderByIdAsc()
                : servicioRepository.findByActivoOrderByIdAsc(filtro);
        return servicios.stream().map(ServicioDTO::de).toList();
    }

    @Transactional
    public ServicioDTO crear(ServicioRequestDTO peticion) {
        String nombre = normalizarNombre(peticion.getNombre());

        // Unicidad del contrato (409 Conflict); las restricciones UNIQUE de la
        // tabla quedan como respaldo ante dos creaciones simultaneas
        if (servicioRepository.existsByCodigoPrefijo(peticion.getCodigoPrefijo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un servicio con el prefijo " + peticion.getCodigoPrefijo());
        }
        if (servicioRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un servicio con ese nombre");
        }

        Servicio servicio = new Servicio();
        servicio.setCodigoPrefijo(peticion.getCodigoPrefijo());
        servicio.setNombre(nombre);
        servicio.setDescripcion(normalizarDescripcion(peticion.getDescripcion()));
        // Todo servicio nuevo nace activo (contrato del endpoint)
        servicio.setActivo(true);
        Servicio guardado = servicioRepository.save(servicio);

        // Fila 1:1 de control de cola: misma transaccion que el alta del servicio
        EstadoColaServicio estado = new EstadoColaServicio();
        estado.setServicioId(guardado.getId());
        estado.setFechaOperacion(LocalDate.now(ZonaHoraria.ZONA));
        estado.setUltimoConsecutivo(0);
        estado.setContadorPreferentes(0);
        estado.setUmbralEnvejecimientoMin(UMBRAL_ENVEJECIMIENTO_DEFECTO);
        estado.setProporcionPreferente(PROPORCION_PREFERENTE_DEFECTO);
        estadoColaServicioRepository.save(estado);

        return ServicioDTO.de(guardado);
    }

    @Transactional
    public ServicioDTO actualizar(Long id, ServicioUpdateDTO peticion) {
        String nombre = normalizarNombre(peticion.getNombre());

        // Orden global de bloqueos del contrato: primero el servicio, despues
        // todos sus modulos en orden ascendente de id y al final su estado de
        // cola. Se revalidan las condiciones ya con los bloqueos tomados.
        Servicio servicio = servicioRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        List<ModuloAtencion> modulos = moduloAtencionRepository.bloquearPorServicioId(id);
        estadoColaServicioRepository.bloquearPorServicioId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "El servicio no tiene fila de estado de cola"));

        if (servicioRepository.existsByNombreAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un servicio con ese nombre");
        }

        boolean seDesactiva = Boolean.TRUE.equals(servicio.getActivo())
                && Boolean.FALSE.equals(peticion.getActivo());

        if (seDesactiva) {
            LocalDate hoy = LocalDate.now(ZonaHoraria.ZONA);
            boolean enEsperaHoy = turnoRepository.existsByServicioIdAndFechaOperacionAndEstado(
                    id, hoy, EstadoTurno.EN_ESPERA);
            boolean turnoActivo = turnoRepository.existsByServicioIdAndEstadoIn(id, ESTADOS_ACTIVOS);
            if (enEsperaHoy || turnoActivo) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El servicio tiene turnos pendientes o activos; resuélvalos antes de desactivarlo");
            }
            // Cascada del contrato. Las entidades ya estan administradas y
            // bloqueadas: el flush de la transaccion persiste activo = false.
            modulos.forEach(modulo -> modulo.setActivo(false));
        }

        servicio.setNombre(nombre);
        servicio.setDescripcion(normalizarDescripcion(peticion.getDescripcion()));
        // Al reactivar, los modulos siguen inactivos (no se reactivan aca)
        servicio.setActivo(peticion.getActivo());

        return ServicioDTO.de(servicioRepository.save(servicio));
    }

    // Nombre obligatorio de 3 a 100 caracteres, sin espacios exteriores
    private String normalizarNombre(String nombre) {
        String recortado = nombre.trim();
        if (recortado.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 3 caracteres");
        }
        return recortado;
    }

    // Descripcion opcional: el texto vacio se guarda como null
    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String recortada = descripcion.trim();
        return recortada.isEmpty() ? null : recortada;
    }
}