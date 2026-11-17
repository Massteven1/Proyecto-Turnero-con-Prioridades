package com.banco.app.dto.response;

import java.time.LocalDate;

// Salida de GET /api/v1/turnos/indicadores (catalogo de DTOs #29).
// - fechaConsulta es la fecha efectiva del filtro: si el cliente omite fecha,
//   el backend usa el dia actual de America/Bogota.
// - servicioId es null cuando el indicador es global.
// - totalEmitidos cuenta todos los turnos del alcance, en cualquier estado; los
//   seis conteos por estado lo desglosan.
// - tiempoPromedioEsperaSegundos solo promedia turnos con fecha de inicio de
//   atencion y conserva los decimales; el texto redondea al segundo entero.
//   Si ningun turno del alcance fue atendido, ambos campos son null: no hay
//   promedio (distinto de un promedio de cero segundos).
public class IndicadoresDTO {

    private LocalDate fechaConsulta;
    private Long servicioId;
    private Long totalEmitidos;
    private Long totalFinalizados;
    private Long totalCancelados;
    private Long totalAusentes;
    private Long totalEnEspera;
    private Long totalEnAtencionOLlamado;
    private Double tiempoPromedioEsperaSegundos;
    private String tiempoPromedioEsperaFormateado;

    public static IndicadoresDTO de(LocalDate fechaConsulta, Long servicioId, long totalEmitidos,
            long totalFinalizados, long totalCancelados, long totalAusentes, long totalEnEspera,
            long totalEnAtencionOLlamado, Double tiempoPromedioEsperaSegundos) {
        IndicadoresDTO dto = new IndicadoresDTO();
        dto.fechaConsulta = fechaConsulta;
        dto.servicioId = servicioId;
        dto.totalEmitidos = totalEmitidos;
        dto.totalFinalizados = totalFinalizados;
        dto.totalCancelados = totalCancelados;
        dto.totalAusentes = totalAusentes;
        dto.totalEnEspera = totalEnEspera;
        dto.totalEnAtencionOLlamado = totalEnAtencionOLlamado;
        dto.tiempoPromedioEsperaSegundos = tiempoPromedioEsperaSegundos;
        dto.tiempoPromedioEsperaFormateado = formatear(tiempoPromedioEsperaSegundos);
        return dto;
    }

    // "08 min 25 seg": se redondea al segundo entero y se rellenan minutos y
    // segundos a dos digitos; un promedio de mas de 99 minutos crece solo.
    private static String formatear(Double segundos) {
        if (segundos == null) {
            return null;
        }
        long total = Math.round(segundos);
        return String.format("%02d min %02d seg", total / 60, total % 60);
    }

    public LocalDate getFechaConsulta() {
        return fechaConsulta;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public Long getTotalEmitidos() {
        return totalEmitidos;
    }

    public Long getTotalFinalizados() {
        return totalFinalizados;
    }

    public Long getTotalCancelados() {
        return totalCancelados;
    }

    public Long getTotalAusentes() {
        return totalAusentes;
    }

    public Long getTotalEnEspera() {
        return totalEnEspera;
    }

    public Long getTotalEnAtencionOLlamado() {
        return totalEnAtencionOLlamado;
    }

    public Double getTiempoPromedioEsperaSegundos() {
        return tiempoPromedioEsperaSegundos;
    }

    public String getTiempoPromedioEsperaFormateado() {
        return tiempoPromedioEsperaFormateado;
    }
}
