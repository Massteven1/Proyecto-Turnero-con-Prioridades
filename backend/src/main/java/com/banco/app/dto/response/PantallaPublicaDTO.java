package com.banco.app.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Salida de GET /api/v1/publico/pantalla (catalogo de DTOs #25).
// - turnosEnModulos: solo LLAMADO / EN_ATENCION con modulo asignado.
// - proximosEnEspera: hasta 10 turnos EN_ESPERA por servicio, ordenados por la
//   proyeccion de llamados sucesivos.
// - horaServidor es la hora unica con la que se calculo toda la pantalla.
// No incluye identificadorCliente ni estados terminales.
public class PantallaPublicaDTO {

    private LocalDate fechaOperacion;
    private LocalDateTime horaServidor;
    private List<TurnoPublicoModuloDTO> turnosEnModulos;
    private List<TurnoPublicoColaDTO> proximosEnEspera;

    public static PantallaPublicaDTO de(LocalDate fechaOperacion, LocalDateTime horaServidor,
            List<TurnoPublicoModuloDTO> turnosEnModulos,
            List<TurnoPublicoColaDTO> proximosEnEspera) {
        PantallaPublicaDTO dto = new PantallaPublicaDTO();
        dto.fechaOperacion = fechaOperacion;
        dto.horaServidor = horaServidor;
        dto.turnosEnModulos = turnosEnModulos;
        dto.proximosEnEspera = proximosEnEspera;
        return dto;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public LocalDateTime getHoraServidor() {
        return horaServidor;
    }

    public List<TurnoPublicoModuloDTO> getTurnosEnModulos() {
        return turnosEnModulos;
    }

    public List<TurnoPublicoColaDTO> getProximosEnEspera() {
        return proximosEnEspera;
    }
}
