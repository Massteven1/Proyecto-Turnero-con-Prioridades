package com.banco.app.repository;

import com.banco.app.entity.Servicio;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla servicios (catalogo de areas de atencion bancaria).
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    // Unicidad del prefijo y del nombre al crear/actualizar (409 Conflict)
    boolean existsByCodigoPrefijo(String codigoPrefijo);

    boolean existsByNombre(String nombre);

    // Unicidad del nombre al actualizar: el propio servicio no cuenta
    boolean existsByNombreAndIdNot(String nombre, Long id);

    List<Servicio> findAllByOrderByIdAsc();

    List<Servicio> findByActivoOrderByIdAsc(Boolean activo);

    // Bloqueo pesimista de la fila del servicio, primer eslabon del orden global
    // de bloqueos. SQL nativo a proposito: Hibernate 7 genera "FOR UPDATE OF
    // <alias>" en HQL y MariaDB no acepta esa clausula (error de sintaxis).
    @Query(value = "SELECT * FROM servicios WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Servicio> findByIdForUpdate(@Param("id") Long id);

    // Bloquea en UNA sola sentencia los servicios implicados y en orden
    // ascendente de id, como exige el contrato: dos peticiones simultaneas que
    // tocan los mismos servicios deben adquirir los bloqueos en el mismo orden
    // para no interbloquearse.
    @Query(value = "SELECT * FROM servicios WHERE id IN (:ids) ORDER BY id ASC FOR UPDATE",
            nativeQuery = true)
    List<Servicio> bloquearPorIds(@Param("ids") Collection<Long> ids);
}