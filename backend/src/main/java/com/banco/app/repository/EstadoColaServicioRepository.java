package com.banco.app.repository;

import com.banco.app.entity.EstadoColaServicio;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

// Acceso a la tabla estado_cola_servicio (una fila 1:1 por servicio).
// La fila con bloqueo PESSIMISTIC_WRITE es la pieza central de la atomicidad:
// serializa la emision de consecutivos y el algoritmo de seleccion de turnos.
public interface EstadoColaServicioRepository extends JpaRepository<EstadoColaServicio, Long> {

    Optional<EstadoColaServicio> findByServicioId(Long servicioId);

    // Version con bloqueo pesimista para las transacciones de turno (Fases 3 y 4).
    // Spring Data no deriva queries del nombre para metodos con @Lock personalizado,
    // por eso se escribe la consulta JPQL de forma explicita.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EstadoColaServicio e where e.servicioId = :servicioId")
    Optional<EstadoColaServicio> bloquearPorServicioId(@Param("servicioId") Long servicioId);
}
