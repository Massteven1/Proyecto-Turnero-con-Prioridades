package com.banco.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Cuerpo del PUT /api/v1/parametros-cola/{servicioId} (contrato #14).
// umbral: minutos para que un turno Regular se considere envejecido (1-240).
// proporcion: llamados preferentes por cada regular (1-10).
public class ParametrosColaDTO {

    @NotNull(message = "El umbral de envejecimiento es obligatorio")
    @Min(value = 1, message = "El umbral de envejecimiento debe ser al menos 1 minuto")
    @Max(value = 240, message = "El umbral de envejecimiento no debe exceder 240 minutos")
    private Integer umbralEnvejecimientoMin;

    @NotNull(message = "La proporción preferente es obligatoria")
    @Min(value = 1, message = "La proporción preferente debe ser al menos 1")
    @Max(value = 10, message = "La proporción preferente no debe exceder 10")
    private Integer proporcionPreferente;

    public Integer getUmbralEnvejecimientoMin() {
        return umbralEnvejecimientoMin;
    }

    public void setUmbralEnvejecimientoMin(Integer umbralEnvejecimientoMin) {
        this.umbralEnvejecimientoMin = umbralEnvejecimientoMin;
    }

    public Integer getProporcionPreferente() {
        return proporcionPreferente;
    }

    public void setProporcionPreferente(Integer proporcionPreferente) {
        this.proporcionPreferente = proporcionPreferente;
    }
}