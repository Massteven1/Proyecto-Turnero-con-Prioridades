package com.banco.app.service;

import com.banco.app.config.ZonaHoraria;
import com.banco.app.dto.response.PantallaPublicaDTO;
import com.banco.app.dto.response.TurnoColaDTO;
import com.banco.app.dto.response.TurnoPublicoColaDTO;
import com.banco.app.dto.response.TurnoPublicoModuloDTO;
import com.banco.app.dto.response.TurnoResumenDTO;
import com.banco.app.entity.Categoria;
import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import com.banco.app.repository.CategoriaRepository;
import com.banco.app.repository.EstadoColaServicioRepository;
import com.banco.app.repository.TurnoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Consultas de solo lectura sobre turnos: GET /api/v1/turnos (modo=COLA y
// modo=HISTORIAL) y GET /api/v1/publico/pantalla. Contrato de las filas 24 y 22
// del xlsx y de las secciones 4 y 5 de la especificacion.
//
// Nada de esta clase escribe en la base: la proyeccion de la cola se calcula en
// memoria con una copia del contador y de los parametros vigentes.
@Service
public class TurnoConsultaService {

    // La pantalla publica muestra como maximo 10 turnos EN_ESPERA por servicio.
    private static final int MAXIMO_PANTALLA_POR_SERVICIO = 10;

    private static final List<EstadoTurno> ESTADOS_EN_MODULO =
            List.of(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);

    private final TurnoRepository turnoRepository;
    private final CategoriaRepository categoriaRepository;
    private final EstadoColaServicioRepository estadoColaServicioRepository;

    public TurnoConsultaService(TurnoRepository turnoRepository,
            CategoriaRepository categoriaRepository,
            EstadoColaServicioRepository estadoColaServicioRepository) {
        this.turnoRepository = turnoRepository;
        this.categoriaRepository = categoriaRepository;
        this.estadoColaServicioRepository = estadoColaServicioRepository;
    }

    // GET /api/v1/turnos?modo=COLA&servicioId={id}
    // La cola es siempre la del dia actual y solo EN_ESPERA. fecha y estado no se
    // admiten en este modo (400); servicioId es obligatorio (400).
    @Transactional(readOnly = true)
    public List<TurnoColaDTO> cola(Long servicioId, LocalDate fecha, EstadoTurno estado) {
        if (fecha != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El modo=COLA no admite el filtro fecha; la cola es siempre la del día actual");
        }
        if (estado != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El modo=COLA no admite el filtro estado; la cola solo contiene turnos EN_ESPERA");
        }
        if (servicioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El servicioId es obligatorio en modo=COLA");
        }

        // La fila 1:1 de estado de cola existe desde el alta del servicio: si no
        // aparece, el servicio no existe.
        EstadoColaServicio configuracion = estadoColaServicioRepository.findByServicioId(servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Servicio no encontrado"));

        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.ZONA);
        List<Turno> cola = turnoRepository.buscarEnEsperaDelDiaPorServicio(
                ahora.toLocalDate(), EstadoTurno.EN_ESPERA, servicioId);
        if (cola.isEmpty()) {
            return List.of();
        }

        List<ProyeccionCola.Item> proyeccion = ProyeccionCola.proyectar(cola, categorias(),
                configuracion.getUmbralEnvejecimientoMin(),
                configuracion.getProporcionPreferente(),
                configuracion.getContadorPreferentes(), ahora);

        return proyeccion.stream()
                .map(item -> TurnoColaDTO.de(item.turno(), item.ordenEnCola(),
                        item.preferenteOriginal(), item.envejecido(), ahora))
                .toList();
    }

    // GET /api/v1/turnos?modo=HISTORIAL&fecha=&servicioId=&estado=
    // Exclusivo ADMIN: un OPERADOR recibe 403 (contrato de la fila 24). Todos los
    // filtros son opcionales; sin fecha se consultan todos los dias. El orden es
    // del mas reciente al mas antiguo, con desempate por id.
    @Transactional(readOnly = true)
    public List<TurnoResumenDTO> historial(LocalDate fecha, Long servicioId, EstadoTurno estado,
            boolean administrador) {
        if (!administrador) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "El historial de turnos es exclusivo del rol ADMIN");
        }
        return turnoRepository.buscarHistorial(fecha, servicioId, estado).stream()
                .map(TurnoResumenDTO::de)
                .toList();
    }

    // GET /api/v1/publico/pantalla (sin JWT).
    // turnosEnModulos: LLAMADO / EN_ATENCION del dia con modulo asignado.
    // proximosEnEspera: hasta 10 EN_ESPERA por servicio, en el orden simulado.
    @Transactional(readOnly = true)
    public PantallaPublicaDTO pantalla() {
        LocalDateTime ahora = LocalDateTime.now(ZonaHoraria.ZONA);
        LocalDate hoy = ahora.toLocalDate();

        List<TurnoPublicoModuloDTO> enModulos = turnoRepository
                .buscarActivosDelDia(hoy, ESTADOS_EN_MODULO).stream()
                .filter(turno -> turno.getModuloId() != null)
                .map(TurnoPublicoModuloDTO::de)
                .toList();

        List<Turno> enEspera = turnoRepository.buscarEnEsperaDelDia(hoy, EstadoTurno.EN_ESPERA);
        return PantallaPublicaDTO.de(hoy, ahora, enModulos, proximosEnEspera(enEspera, ahora));
    }

    private List<TurnoPublicoColaDTO> proximosEnEspera(List<Turno> enEspera, LocalDateTime ahora) {
        if (enEspera.isEmpty()) {
            return List.of();
        }

        // Un grupo por servicio conservando el orden FIFO que ya trae la consulta.
        Map<Long, List<Turno>> porServicio = new LinkedHashMap<>();
        for (Turno turno : enEspera) {
            porServicio.computeIfAbsent(turno.getServicioId(), clave -> new ArrayList<>()).add(turno);
        }

        Map<Long, EstadoColaServicio> configuraciones = new HashMap<>();
        estadoColaServicioRepository.listarConServicio()
                .forEach(configuracion -> configuraciones.put(configuracion.getServicioId(), configuracion));

        Map<Long, Categoria> categorias = categorias();
        List<TurnoPublicoColaDTO> proximos = new ArrayList<>();
        // Servicios en orden ascendente para que la pantalla sea estable.
        for (Long servicioId : porServicio.keySet().stream().sorted().toList()) {
            EstadoColaServicio configuracion = configuraciones.get(servicioId);
            if (configuracion == null) {
                // El esquema garantiza la fila 1:1; sin parametros no se proyecta.
                continue;
            }
            List<ProyeccionCola.Item> proyeccion = ProyeccionCola.proyectar(
                    porServicio.get(servicioId), categorias,
                    configuracion.getUmbralEnvejecimientoMin(),
                    configuracion.getProporcionPreferente(),
                    configuracion.getContadorPreferentes(), ahora);

            proyeccion.stream()
                    .limit(MAXIMO_PANTALLA_POR_SERVICIO)
                    .forEach(item -> proximos.add(TurnoPublicoColaDTO.de(
                            item.turno().getCodigo(),
                            item.turno().getServicio().getNombre(),
                            item.turno().getEstado(),
                            item.ordenEnCola())));
        }
        return proximos;
    }

    // Las cuatro categorias base caben en una sola lectura y filtran el
    // envejecimiento de la proyeccion.
    private Map<Long, Categoria> categorias() {
        Map<Long, Categoria> categorias = new HashMap<>();
        categoriaRepository.findAll()
                .forEach(categoria -> categorias.put(categoria.getId(), categoria));
        return categorias;
    }
}
