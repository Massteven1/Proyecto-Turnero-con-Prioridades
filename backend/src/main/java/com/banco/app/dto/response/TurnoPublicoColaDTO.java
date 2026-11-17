package com.banco.app.dto.response;

import com.banco.app.entity.EstadoTurno;

// Turno EN_ESPERA que la pantalla publica proyecta como proximo llamado.
// ordenEnCola se reinicia en 1 para cada servicio y refleja la simulacion del
// algoritmo vigente con la hora unica de la consulta; la pantalla no muestra
// identificadorCliente ni la categoria del cliente.
public class TurnoPublicoColaDTO {

    private Integer ordenEnCola;
    private String codigo;
    private String servicioNombre;
    private EstadoTurno estado;

    public static TurnoPublicoColaDTO de(String codigo, String servicioNombre,
            EstadoTurno estado, int ordenEnCola) {
        TurnoPublicoColaDTO dto = new TurnoPublicoColaDTO();
        dto.ordenEnCola = ordenEnCola;
        dto.codigo = codigo;
        dto.servicioNombre = servicioNombre;
        dto.estado = estado;
        return dto;
    }

    public Integer getOrdenEnCola() {
        return ordenEnCola;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public EstadoTurno getEstado() {
        return estado;
    }
}
