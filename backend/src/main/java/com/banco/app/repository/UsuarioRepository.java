package com.banco.app.repository;

import com.banco.app.entity.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

// Acceso a la tabla usuarios. Las consultas se agregan por fase a medida
// que las necesitan los servicios.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Login (Fase 1) y filtro JWT: buscar la cuenta por username exacto
    Optional<Usuario> findByUsername(String username);

    // Validar unicidad al crear/consultar
    boolean existsByUsername(String username);
}
