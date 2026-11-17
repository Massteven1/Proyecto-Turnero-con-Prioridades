package com.banco.app.dto.response;

import com.banco.app.entity.Categoria;
import com.banco.app.entity.ModuloAtencion;
import com.banco.app.entity.Servicio;
import com.banco.app.entity.Turno;
import com.banco.app.entity.Usuario;
import java.time.LocalDateTime;
import java.util.List;

// Salida de GET /api/v1/turnos/{id} (catalogo de DTOs #27): "todos los campos de
// TurnoDTO y transiciones al mismo nivel". Hereda el mapeo de TurnoDTO para no
// duplicar los veinticuatro campos y agrega la lista cronologica del historial.
// - identificadorCliente viaja porque la ruta exige JWT.
// - Las transiciones van de la mas antigua a la mas reciente (fechaCambio, id).
public class TurnoDetalleDTO extends TurnoDTO {

    private List<TransicionHistorialDTO> transiciones;

    public static TurnoDetalleDTO de(Turno turno, Servicio servicio, Categoria categoria,
            ModuloAtencion modulo, Usuario operador, int umbralEnvejecimientoMin,
            LocalDateTime ahora, List<TransicionHistorialDTO> transiciones) {
        TurnoDetalleDTO dto = new TurnoDetalleDTO();
        dto.completar(turno, servicio, categoria, modulo, operador, umbralEnvejecimientoMin, ahora);
        dto.transiciones = transiciones;
        return dto;
    }

    public List<TransicionHistorialDTO> getTransiciones() {
        return transiciones;
    }
}
