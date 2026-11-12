package com.banco.app.repository;

import com.banco.app.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla categorias (las 4 categorias base son inmutables salvo el nombre).
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Unicidad del nombre visible al renombrar (comparacion en service)
    boolean existsByNombreIgnoreCase(String nombre);
}
