package com.banco.app.repository;

import com.banco.app.entity.HistorialTurno;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla historial_turnos (auditoria inmutable de transiciones).
// Escritura: solo INSERT desde TurnoService; nunca UPDATE ni DELETE.
public interface HistorialTurnoRepository extends JpaRepository<HistorialTurno, Long> {

    // Transiciones cronologicas de un turno para GET /api/v1/turnos/{id}.
    // El join fetch trae al actor en la misma consulta: sin el, cada fila del
    // historial dispararia una lectura extra de usuarios.
    @Query("""
            select h from HistorialTurno h
                join fetch h.operador
            where h.turnoId = :turnoId
            order by h.fechaCambio asc, h.id asc
            """)
    List<HistorialTurno> buscarTransicionesDeTurno(@Param("turnoId") Long turnoId);
}
