package com.banco.app.repository;

import com.banco.app.entity.ModuloAtencion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla modulos_atencion (ventanillas por servicio).
public interface ModuloAtencionRepository extends JpaRepository<ModuloAtencion, Long> {

    // El nombre del modulo es unico globalmente (la normalizacion de mayusculas
    // y espacios exteriores se aplica en la capa service antes de consultar)
    boolean existsByNombreIgnoreCase(String nombre);

    List<ModuloAtencion> findByServicioId(Long servicioId);
}
