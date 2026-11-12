package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Area de atencion bancaria (ej. "Caja General" con prefijo CAJ).
// codigo_prefijo forma parte del codigo visible del turno (CAJ-20261105-001)
// y por eso es INMUTABLE despues de crear el servicio.
// Al crearse, el backend inserta tambien su fila 1:1 en estado_cola_servicio.
@Entity
@Table(name = "servicios")
public class Servicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_prefijo", nullable = false, unique = true)
    private String codigoPrefijo;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    // Baja logica: no se desactiva si tiene turnos pendientes/activos (lo valida service)
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoPrefijo() {
        return codigoPrefijo;
    }

    public void setCodigoPrefijo(String codigoPrefijo) {
        this.codigoPrefijo = codigoPrefijo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
