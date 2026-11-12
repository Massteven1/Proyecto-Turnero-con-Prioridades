package com.banco.app.repository;

import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla usuarios. Las consultas se agregan por fase a medida
// que las necesitan los servicios.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Login (Fase 1) y filtro JWT: buscar la cuenta por username exacto
    Optional<Usuario> findByUsername(String username);

    // Validar unicidad al crear/consultar
    boolean existsByUsername(String username);

    // Listado del CRUD con orden estable por id
    List<Usuario> findAllByOrderByIdAsc();

    List<Usuario> findByActivoOrderByIdAsc(Boolean activo);

    // PUT /usuarios/{id}: bloquea la cuenta objetivo durante la transaccion
    // antes de leer su estado vigente (contrato del endpoint).
    // SQL nativo a proposito: Hibernate 7 genera "FOR UPDATE OF <alias>" en
    // HQL y MariaDB no acepta esa clausula (error de sintaxis).
    @Query(value = "SELECT * FROM usuarios WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Usuario> findByIdForUpdate(@Param("id") Long id);

    // Serializa los cambios que pueden reducir la cantidad de ADMIN bloqueando
    // en UNA sola sentencia, y en orden ascendente de id, la cuenta objetivo y
    // todas las cuentas ADMIN activas. El contrato exige ese orden para que dos
    // actualizaciones simultaneas no se interbloqueen ni degraden al ultimo ADMIN.
    // Bloquear el conjunto completo siempre (y no solo la cuenta objetivo) evita
    // la carrera en la que la cuenta cambia de rol entre la lectura y el bloqueo.
    // La tabla solo tiene PK y uk_username, asi que MariaDB recorre el indice
    // primario y adquiere los bloqueos en el orden del ORDER BY.
    // SQL nativo a proposito: Hibernate 7 genera "FOR UPDATE OF <alias>" en HQL
    // y MariaDB no acepta esa clausula (error de sintaxis).
    @Query(value = "SELECT * FROM usuarios WHERE id = :id OR (rol = 'ADMIN' AND activo = 1) "
            + "ORDER BY id ASC FOR UPDATE", nativeQuery = true)
    List<Usuario> bloquearUsuarioYAdminsActivos(@Param("id") Long id);

    long countByRolAndActivo(RolUsuario rol, Boolean activo);
}
