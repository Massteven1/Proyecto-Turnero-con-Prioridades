package com.banco.app.repository;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla turnos. Las consultas de cola/proyeccion/historial
// se agregan a medida que las pide cada endpoint.
public interface TurnoRepository extends JpaRepository<Turno, Long> {

    // Regla del API: si el modulo ya tiene turnos asociados, su servicio_id es inmutable
    boolean existsByModuloId(Long moduloId);

    // PUT /usuarios/{id}: no se puede desactivar ni cambiar el rol de un
    // usuario que mantenga un turno activo (LLAMADO o EN_ATENCION)
    boolean existsByOperadorIdAndEstadoIn(Long operadorId, Collection<EstadoTurno> estados);

    // PUT /servicios/{id}: cola pendiente del dia que impide la baja del servicio
    boolean existsByServicioIdAndFechaOperacionAndEstado(Long servicioId, LocalDate fechaOperacion,
            EstadoTurno estado);

    // PUT /servicios/{id}: turnos activos de cualquier fecha que impiden la baja
    boolean existsByServicioIdAndEstadoIn(Long servicioId, Collection<EstadoTurno> estados);

    // PUT /modulos/{id}: un modulo ocupado no puede desactivarse
    boolean existsByModuloIdAndEstadoIn(Long moduloId, Collection<EstadoTurno> estados);

    // Cola EN_ESPERA de un servicio ordenada FIFO con desempate por id ASC
    List<Turno> findByServicioIdAndFechaOperacionAndEstadoOrderByFechaIngresoAscIdAsc(
            Long servicioId, LocalDate fechaOperacion, EstadoTurno estado);

    // GET /modulos: turno activo (LLAMADO o EN_ATENCION) por modulo, para
    // calcular "ocupado" y "turnoActivoCodigo" sin una consulta por fila
    @Query("""
            select t.moduloId as moduloId, t.codigo as codigo
            from Turno t
            where t.moduloId in :moduloIds
              and t.estado in :estados
            """)
    List<TurnoActivoView> buscarTurnosActivos(@Param("moduloIds") Collection<Long> moduloIds,
            @Param("estados") Collection<EstadoTurno> estados);
}