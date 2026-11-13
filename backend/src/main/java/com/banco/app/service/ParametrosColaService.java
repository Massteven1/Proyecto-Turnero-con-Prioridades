package com.banco.app.service;

import com.banco.app.dto.request.ParametrosColaDTO;
import com.banco.app.dto.response.EstadoColaDTO;
import com.banco.app.entity.EstadoColaServicio;
import com.banco.app.entity.Servicio;
import com.banco.app.repository.EstadoColaServicioRepository;
import com.banco.app.repository.ServicioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Parametros de cola por servicio (contrato GET/PUT /api/v1/parametros-cola).
// El PUT bloquea primero el servicio y despues la fila de estado de cola,
// respetando el orden global, y ajusta el contador de preferentes al nuevo
// limite si la proporcion disminuye.
@Service
public class ParametrosColaService {

    private final EstadoColaServicioRepository estadoColaServicioRepository;
    private final ServicioRepository servicioRepository;

    public ParametrosColaService(EstadoColaServicioRepository estadoColaServicioRepository,
            ServicioRepository servicioRepository) {
        this.estadoColaServicioRepository = estadoColaServicioRepository;
        this.servicioRepository = servicioRepository;
    }

    @Transactional(readOnly = true)
    public List<EstadoColaDTO> listar() {
        return estadoColaServicioRepository.listarConServicio().stream()
                .map(EstadoColaDTO::de)
                .toList();
    }

    @Transactional
    public EstadoColaDTO actualizar(Long servicioId, ParametrosColaDTO peticion) {
        // Orden global de bloqueos: servicio y despues su estado de cola
        Servicio servicio = servicioRepository.findByIdForUpdate(servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Servicio no encontrado"));
        EstadoColaServicio estado = estadoColaServicioRepository.bloquearPorServicioId(servicioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El servicio no tiene estado de cola configurado"));

        estado.setUmbralEnvejecimientoMin(peticion.getUmbralEnvejecimientoMin());
        estado.setProporcionPreferente(peticion.getProporcionPreferente());

        // Si la nueva proporcion es menor que el contador vigente, se reajusta
        // al nuevo maximo (regla del contrato)
        if (estado.getContadorPreferentes() > peticion.getProporcionPreferente()) {
            estado.setContadorPreferentes(peticion.getProporcionPreferente());
        }

        estadoColaServicioRepository.save(estado);
        return EstadoColaDTO.de(estado, servicio);
    }
}