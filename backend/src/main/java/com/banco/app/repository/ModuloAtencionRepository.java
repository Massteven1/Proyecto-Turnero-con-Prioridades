package com.banco.app.repository;

import com.banco.app.entity.ModuloAtencion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla modulos_atencion (ventanillas por servicio).
public interface ModuloAtencionRepository extends JpaRepository<ModuloAtencion, Long> {

    List<ModuloAtencion> findByServicioId(Long servicioId);

    // Cantidad de modulos activos de un servicio: sostiene la regla del ultimo
    // modulo activo cuando todavia hay turnos EN_ESPERA en la cola.
    long countByServicioIdAndActivo(Long servicioId, Boolean activo);

    // Listado del contrato: filtros opcionales por servicio y estado. El
    // join fetch trae el servicio en la misma consulta (el DTO lo necesita).
    @Query("""
            select m from ModuloAtencion m
            join fetch m.servicio s
            where (:servicioId is null or m.servicioId = :servicioId)
              and (:activo is null or m.activo = :activo)
            order by m.id asc
            """)
    List<ModuloAtencion> buscar(@Param("servicioId") Long servicioId,
            @Param("activo") Boolean activo);

    // Bloqueo pesimista de la fila del modulo, siempre despues de los servicios
    // implicados y antes de las filas de estado de cola (orden global).
    @Query(value = "SELECT * FROM modulos_atencion WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<ModuloAtencion> findByIdForUpdate(@Param("id") Long id);

    // PUT /servicios/{id}: al desactivar un servicio se desactivan en cascada
    // todos sus modulos, por eso se bloquean en orden ascendente de id.
    @Query(value = "SELECT * FROM modulos_atencion WHERE servicio_id = :servicioId "
            + "ORDER BY id ASC FOR UPDATE", nativeQuery = true)
    List<ModuloAtencion> bloquearPorServicioId(@Param("servicioId") Long servicioId);

    // Unicidad global del nombre ignorando mayusculas/minusculas y espacios
    // exteriores (TRIM + UPPER en SQL). Se excluye el propio modulo al
    // actualizar; :idExcluido llega null al crear.
    @Query(value = """
            SELECT COUNT(*) FROM modulos_atencion
            WHERE UPPER(TRIM(nombre)) = UPPER(TRIM(:nombre))
              AND (:idExcluido IS NULL OR id <> :idExcluido)
            """, nativeQuery = true)
    long contarNombreNormalizado(@Param("nombre") String nombre,
            @Param("idExcluido") Long idExcluido);
}