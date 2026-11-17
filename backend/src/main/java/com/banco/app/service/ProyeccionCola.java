package com.banco.app.service;

import com.banco.app.entity.Categoria;
import com.banco.app.entity.Turno;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Simulacion de llamados sucesivos sobre una instantanea de la cola.
// Es la MISMA seleccion que aplica POST /api/v1/modulos/{id}/siguiente, pero en
// memoria: recibe la cola EN_ESPERA ya ordenada (fechaIngreso ASC, id ASC), el
// umbral y la proporcion vigentes y el contador persistido, y devuelve el orden
// proyectado sin tocar la base.
//
// Supuestos del contrato: modulos disponibles y ausencia de nuevas operaciones
// mientras dura la simulacion. La hora de consulta es UNICA para toda la
// proyeccion: de ella salen el envejecimiento y el tiempo de espera.
//
// Coste: cada paso reclasifica los pendientes, O(n^2) sobre una cola pequena.
final class ProyeccionCola {

    // ordenEnCola arranca en 1 y es la posicion simulada del turno.
    record Item(Turno turno, int ordenEnCola, boolean preferenteOriginal, boolean envejecido) {

        boolean preferenteEfectivo() {
            return preferenteOriginal || envejecido;
        }
    }

    private ProyeccionCola() {
        // Clase de utilidades: no se instancia
    }

    static List<Item> proyectar(List<Turno> cola, Map<Long, Categoria> categorias,
            int umbralEnvejecimientoMin, int proporcionPreferente, int contadorInicial,
            LocalDateTime ahora) {
        List<Item> pendientes = new ArrayList<>(cola.size());
        for (Turno turno : cola) {
            boolean original = esPreferenteOriginal(turno, categorias);
            boolean envejecido = !original && esperaMinutos(turno, ahora) >= umbralEnvejecimientoMin;
            pendientes.add(new Item(turno, 0, original, envejecido));
        }

        List<Item> proyeccion = new ArrayList<>(pendientes.size());
        int contador = contadorInicial;

        while (!pendientes.isEmpty()) {
            List<Item> preferentes = new ArrayList<>();
            List<Item> regulares = new ArrayList<>();
            for (Item candidato : pendientes) {
                if (candidato.preferenteEfectivo()) {
                    preferentes.add(candidato);
                } else {
                    regulares.add(candidato);
                }
            }

            Item elegido;
            if (!preferentes.isEmpty() && !regulares.isEmpty()) {
                if (contador < proporcionPreferente) {
                    elegido = preferentes.get(0);
                    contador = Math.min(contador + 1, proporcionPreferente);
                } else {
                    elegido = regulares.get(0);
                    contador = 0;
                }
            } else if (!preferentes.isEmpty()) {
                elegido = preferentes.get(0);
                contador = Math.min(contador + 1, proporcionPreferente);
            } else {
                elegido = regulares.get(0);
                contador = 0;
            }

            // La lista de pendientes conserva el orden FIFO de la cola, asi que
            // el primer elemento de cada conjunto es el que corresponde llamar.
            pendientes.remove(elegido);
            proyeccion.add(new Item(elegido.turno(), proyeccion.size() + 1,
                    elegido.preferenteOriginal(), elegido.envejecido()));
        }
        return proyeccion;
    }

    private static boolean esPreferenteOriginal(Turno turno, Map<Long, Categoria> categorias) {
        Categoria categoria = categorias.get(turno.getCategoriaId());
        return categoria != null && Boolean.TRUE.equals(categoria.getPreferente());
    }

    private static long esperaMinutos(Turno turno, LocalDateTime ahora) {
        return Duration.between(turno.getFechaIngreso(), ahora).toMinutes();
    }
}
