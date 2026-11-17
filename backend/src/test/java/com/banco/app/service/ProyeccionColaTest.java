package com.banco.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banco.app.entity.Categoria;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

// El algoritmo de prioridad es la regla mas visible del contrato, asi que se
// prueba sobre la proyeccion en memoria: es exactamente la misma seleccion que
// aplica POST /api/v1/modulos/{id}/siguiente, sin base de datos de por medio.
class ProyeccionColaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 16, 10, 0);
    private static final long CATEGORIA_PREFERENTE = 1L;
    private static final long CATEGORIA_REGULAR = 2L;
    private static final int UMBRAL_MINUTOS = 60;
    private static final int PROPORCION = 3;

    @Test
    @DisplayName("con proporcion 3 deja pasar tres preferentes y despues un regular")
    void alternaTresPreferentesPorCadaRegular() {
        List<Turno> cola = List.of(
                turno(1L, CATEGORIA_PREFERENTE, 1),
                turno(2L, CATEGORIA_REGULAR, 2),
                turno(3L, CATEGORIA_PREFERENTE, 3),
                turno(4L, CATEGORIA_REGULAR, 4),
                turno(5L, CATEGORIA_PREFERENTE, 5),
                turno(6L, CATEGORIA_REGULAR, 6),
                turno(7L, CATEGORIA_REGULAR, 7));

        List<Long> orden = ids(ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA));

        assertThat(orden).containsExactly(1L, 3L, 5L, 2L, 4L, 6L, 7L);
    }

    @Test
    @DisplayName("sin preferentes respeta el FIFO de la cola")
    void sinPreferentesRespetaElFifo() {
        List<Turno> cola = List.of(
                turno(10L, CATEGORIA_REGULAR, 1),
                turno(11L, CATEGORIA_REGULAR, 2),
                turno(12L, CATEGORIA_REGULAR, 3));

        List<Long> orden = ids(ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA));

        assertThat(orden).containsExactly(10L, 11L, 12L);
    }

    @Test
    @DisplayName("un regular que supera el umbral se trata como preferente")
    void regularEnvejecidoPasaAlFrente() {
        List<Turno> cola = List.of(
                turno(20L, CATEGORIA_REGULAR, 1),
                turno(21L, CATEGORIA_REGULAR, 61));

        List<ProyeccionCola.Item> proyeccion =
                ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA);

        assertThat(ids(proyeccion)).containsExactly(21L, 20L);
        assertThat(proyeccion.get(0).envejecido()).isTrue();
        assertThat(proyeccion.get(0).preferenteOriginal()).isFalse();
        assertThat(proyeccion.get(1).envejecido()).isFalse();
    }

    @Test
    @DisplayName("la espera igual al umbral ya cuenta como envejecida")
    void laEsperaExactaDelUmbralEnvejece() {
        List<Turno> cola = List.of(turno(30L, CATEGORIA_REGULAR, UMBRAL_MINUTOS));

        List<ProyeccionCola.Item> proyeccion =
                ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA);

        assertThat(proyeccion.get(0).envejecido()).isTrue();
    }

    @Test
    @DisplayName("el contador persistido continua la proporcion en curso")
    void contadorPersistidoContinuaLaProporcion() {
        List<Turno> cola = List.of(
                turno(40L, CATEGORIA_PREFERENTE, 1),
                turno(41L, CATEGORIA_REGULAR, 2));

        // El contador llego al tope: el siguiente llamado debe ser un regular.
        List<Long> orden = ids(ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, PROPORCION, AHORA));

        assertThat(orden).containsExactly(41L, 40L);
    }

    @Test
    @DisplayName("la proyeccion numera los turnos desde 1 en orden consecutivo")
    void numeraElOrdenEnCola() {
        List<Turno> cola = List.of(
                turno(50L, CATEGORIA_REGULAR, 1),
                turno(51L, CATEGORIA_REGULAR, 2));

        List<ProyeccionCola.Item> proyeccion =
                ProyeccionCola.proyectar(cola, categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA);

        assertThat(proyeccion).extracting(ProyeccionCola.Item::ordenEnCola).containsExactly(1, 2);
    }

    @Test
    @DisplayName("una cola vacia proyecta una lista vacia")
    void colaVaciaProyectaListaVacia() {
        assertThat(ProyeccionCola.proyectar(List.of(), categorias(), UMBRAL_MINUTOS, PROPORCION, 0, AHORA)).isEmpty();
    }

    // Categoria 1 preferente (discapacidad, adulto mayor, ...), categoria 2 regular.
    private static Map<Long, Categoria> categorias() {
        Categoria preferente = new Categoria();
        preferente.setId(CATEGORIA_PREFERENTE);
        preferente.setNombre("Discapacidad");
        preferente.setPreferente(true);
        preferente.setActivo(true);

        Categoria regular = new Categoria();
        regular.setId(CATEGORIA_REGULAR);
        regular.setNombre("Regular");
        regular.setPreferente(false);
        regular.setActivo(true);

        return Map.of(CATEGORIA_PREFERENTE, preferente, CATEGORIA_REGULAR, regular);
    }

    // La cola llega ya ordenada por fechaIngreso ASC, id ASC: el helper solo
    // fija esa antiguedad en minutos para poder controlar el envejecimiento.
    private static Turno turno(Long id, Long categoriaId, int minutosEnEspera) {
        Turno turno = new Turno();
        turno.setId(id);
        turno.setServicioId(1L);
        turno.setCategoriaId(categoriaId);
        turno.setEstado(EstadoTurno.EN_ESPERA);
        turno.setFechaIngreso(AHORA.minusMinutes(minutosEnEspera));
        return turno;
    }

    private static List<Long> ids(List<ProyeccionCola.Item> proyeccion) {
        return proyeccion.stream().map(item -> item.turno().getId()).toList();
    }
}
