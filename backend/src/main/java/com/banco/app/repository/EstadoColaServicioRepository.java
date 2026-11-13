package com.banco.app.repository;

import com.banco.app.entity.EstadoColaServicio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla estado_cola_servicio (una fila 1:1 por servicio).
// La fila con bloqueo pesimista es la pieza central de la atomicidad:
// serializa la emision de consecutivos y el algoritmo de seleccion de turnos.
public interface EstadoColaServicioRepository extends JpaRepository<EstadoColaServicio, Long> {

    Optional<EstadoColaServicio> findByServicioId(Long servicioId);

    // GET /parametros-cola: todas las filas con su servicio en una sola consulta
    @Query("select e from EstadoColaServicio e join fetch e.servicio s order by e.servicioId asc")
    List<EstadoColaServicio> listarConServicio();

    // Version con bloqueo pesimista (Fases 2b, 3 y 4). SQL nativo a proposito:
    // Hibernate 7 genera "FOR UPDATE OF <alias>" en HQL y MariaDB lo rechaza.
    @Query(value = "SELECT * FROM estado_cola_servicio WHERE servicio_id = :servicioId FOR UPDATE",
            nativeQuery = true)
    Optional<EstadoColaServicio> bloquearPorServicioId(@Param("servicioId") Long servicioId);
}