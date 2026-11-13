package com.banco.app.repository;

import com.banco.app.entity.Categoria;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// Acceso a la tabla categorias (las 4 categorias base son inmutables salvo el nombre).
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Unicidad del nombre visible al renombrar (comparacion en service)
    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    // Las cuatro categorias base en el orden del catalogo
    List<Categoria> findAllByOrderByIdAsc();

    // Bloqueo pesimista dentro del orden global (despues de servicios)
    @Query(value = "SELECT * FROM categorias WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Categoria> findByIdForUpdate(@Param("id") Long id);
}