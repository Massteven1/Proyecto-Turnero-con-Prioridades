package com.banco.app.repository;

import com.banco.app.entity.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla servicios (catalogo de areas de atencion bancaria).
public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    // Unicidad del prefijo y del nombre al crear/actualizar (409 Conflict)
    boolean existsByCodigoPrefijo(String codigoPrefijo);

    boolean existsByNombre(String nombre);
}
