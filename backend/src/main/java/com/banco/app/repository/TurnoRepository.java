package com.banco.app.repository;

import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.Turno;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    // Fase 3: bloqueo del turno candidato antes de pasarlo a LLAMADO y del
    // turno objetivo antes de cambiar su estado. SQL nativo a proposito:
    // Hibernate 7 genera "FOR UPDATE OF <alias>" en HQL y MariaDB lo rechaza.
    // Si el turno ya esta administrado en el contexto de persistencia, el
    // bloqueo se toma pero se devuelve la instancia con los valores previos a
    // la espera: desvincularla antes (entityManager.detach) es obligatorio para
    // revalidar con la fila viva.
    @Query(value = "SELECT * FROM turnos WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Turno> findByIdForUpdate(@Param("id") Long id);

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

    // GET /publico/pantalla: cola EN_ESPERA del dia de TODOS los servicios, con
    // el servicio resuelto para no consultarlo fila por fila. El orden FIFO con
    // desempate por id es la entrada de la proyeccion.
    @Query("""
            select t from Turno t
                join fetch t.servicio
            where t.fechaOperacion = :fecha
              and t.estado = :estado
            order by t.fechaIngreso asc, t.id asc
            """)
    List<Turno> buscarEnEsperaDelDia(@Param("fecha") LocalDate fecha,
            @Param("estado") EstadoTurno estado);

    // GET /turnos?modo=COLA: misma cola acotada al servicio pedido.
    @Query("""
            select t from Turno t
                join fetch t.servicio
            where t.fechaOperacion = :fecha
              and t.estado = :estado
              and t.servicioId = :servicioId
            order by t.fechaIngreso asc, t.id asc
            """)
    List<Turno> buscarEnEsperaDelDiaPorServicio(@Param("fecha") LocalDate fecha,
            @Param("estado") EstadoTurno estado, @Param("servicioId") Long servicioId);

    // GET /publico/pantalla: turnos LLAMADO / EN_ATENCION del dia con su modulo.
    @Query("""
            select t from Turno t
                join fetch t.servicio
                left join fetch t.modulo
            where t.fechaOperacion = :fecha
              and t.estado in :estados
            order by t.fechaLlamado asc, t.id asc
            """)
    List<Turno> buscarActivosDelDia(@Param("fecha") LocalDate fecha,
            @Param("estados") Collection<EstadoTurno> estados);

    // GET /turnos?modo=HISTORIAL: filtros opcionales fecha, servicioId y estado.
    // Se ordena del mas reciente al mas antiguo y el id desempata dentro del
    // mismo instante; sin fecha se consultan todos los dias.
    @Query("""
            select t from Turno t
                join fetch t.servicio
                left join fetch t.modulo
                left join fetch t.operador
            where (:fecha is null or t.fechaOperacion = :fecha)
              and (:servicioId is null or t.servicioId = :servicioId)
              and (:estado is null or t.estado = :estado)
            order by t.fechaOperacion desc, t.fechaIngreso desc, t.id desc
            """)
    List<Turno> buscarHistorial(@Param("fecha") LocalDate fecha,
            @Param("servicioId") Long servicioId,
            @Param("estado") EstadoTurno estado);
}