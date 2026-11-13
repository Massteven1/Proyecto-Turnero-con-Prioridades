package com.banco.app.dto.response;

// Salida del POST /api/v1/modulos/{id}/siguiente (contrato del catalogo de DTOs #18).
// Responde 200 OK tanto si se llamo un turno como si la cola estaba vacia:
// en este ultimo caso turno es null y el contador de preferentes no cambia.
public class SiguienteTurnoResponseDTO {

    private Boolean colaVacia;
    private String mensaje;
    private TurnoDTO turno;
    private Integer contadorPreferentesActual;

    public static SiguienteTurnoResponseDTO llamado(String mensaje, TurnoDTO turno, int contadorPreferentesActual) {
        SiguienteTurnoResponseDTO respuesta = new SiguienteTurnoResponseDTO();
        respuesta.colaVacia = false;
        respuesta.mensaje = mensaje;
        respuesta.turno = turno;
        respuesta.contadorPreferentesActual = contadorPreferentesActual;
        return respuesta;
    }

    public static SiguienteTurnoResponseDTO colaVacia(int contadorPreferentesActual) {
        SiguienteTurnoResponseDTO respuesta = new SiguienteTurnoResponseDTO();
        respuesta.colaVacia = true;
        respuesta.mensaje = "No hay turnos en espera para este servicio";
        respuesta.turno = null;
        respuesta.contadorPreferentesActual = contadorPreferentesActual;
        return respuesta;
    }

    public Boolean getColaVacia() {
        return colaVacia;
    }

    public String getMensaje() {
        return mensaje;
    }

    public TurnoDTO getTurno() {
        return turno;
    }

    public Integer getContadorPreferentesActual() {
        return contadorPreferentesActual;
    }
}