package com.banco.app.repository;

import com.banco.app.entity.HistorialTurno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla historial_turnos (auditoria inmutable de transiciones).
// Escritura: solo INSERT desde TurnoService; nunca UPDATE ni DELETE.
public interface HistorialTurnoRepository extends JpaRepository<HistorialTurno, Long> {

    // Transiciones cronologicas de un turno (para GET /api/v1/turnos/{id}, Fase 6)
    List<HistorialTurno> findByTurnoIdOrderByFechaCambioAscIdAsc(Long turnoId);
}
