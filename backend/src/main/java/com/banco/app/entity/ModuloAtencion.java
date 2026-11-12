package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Ventanilla o modulo de atencion asociado a un servicio bancario.
// Los turnos EN_ESPERA pertenecen a la cola del servicio (modulo_id IS NULL):
// solo se vinculan a un modulo cuando pasan a LLAMADO.
@Entity
@Table(name = "modulos_atencion")
public class ModuloAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Id del servicio al que pertenece la ventanilla: es la columna que se escribe.
    // La FK compuesta de turnos (modulo_id, servicio_id) -> modulos (id, servicio_id)
    // impide a nivel SQL asignar un turno a una ventanilla de otro servicio.
    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    // Unico globalmente; la comparacion sin mayusculas/espacios exteriores la hace service
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    // Solo lectura: para armar los DTO con el nombre del servicio sin duplicar datos
    @ManyToOne
    @JoinColumn(name = "servicio_id", insertable = false, updatable = false)
    private Servicio servicio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }
}
