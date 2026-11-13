package com.banco.app.service;

import com.banco.app.dto.request.CategoriaUpdateDTO;
import com.banco.app.dto.response.CategoriaDTO;
import com.banco.app.entity.Categoria;
import com.banco.app.repository.CategoriaRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Catalogo de categorias de prioridad (contrato GET/PUT /api/v1/categorias).
// Las cuatro categorias base no se eliminan, no se desactivan y su atributo
// preferente es inmutable: la API solo permite renombrarlas (409 si el nombre
// ya existe). Los turnos conservan su categoria_nombre_snapshot historico.
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listar() {
        return categoriaRepository.findAllByOrderByIdAsc().stream()
                .map(CategoriaDTO::de)
                .toList();
    }

    @Transactional
    public CategoriaDTO actualizar(Long id, CategoriaUpdateDTO peticion) {
        String nombre = normalizarNombre(peticion.getNombre());

        // Bloqueo de la fila dentro del orden global (despues de servicios)
        Categoria categoria = categoriaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una categoría con ese nombre");
        }

        // Solo cambia el nombre visible: preferente y activo quedan intactos
        categoria.setNombre(nombre);
        return CategoriaDTO.de(categoriaRepository.save(categoria));
    }

    // Nombre obligatorio de 3 a 50 caracteres, sin espacios exteriores
    private String normalizarNombre(String nombre) {
        String recortado = nombre.trim();
        if (recortado.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El nombre debe tener al menos 3 caracteres");
        }
        return recortado;
    }
}