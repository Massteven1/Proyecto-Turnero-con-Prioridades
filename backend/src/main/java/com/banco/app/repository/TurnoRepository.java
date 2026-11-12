package com.banco.app.repository;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla turnos. Las consultas de cola/proyeccion/historial
// se agregan en Fases 3 a 6 a medida que las pide cada endpoint.
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // Regla del API: si el modulo ya tiene turnos asociados, su servicio_id es inmutable
    boolean existsByModuloId(Long moduloId);

    // Cola EN_ESPERA de un servicio ordenada FIFO con desempate por id ASC
    List<Turno> findByServicioIdAndFechaOperacionAndEstadoOrderByFechaIngresoAscIdAsc(
            Long servicioId, java.time.LocalDate fechaOperacion, EstadoTurno estado);
}
