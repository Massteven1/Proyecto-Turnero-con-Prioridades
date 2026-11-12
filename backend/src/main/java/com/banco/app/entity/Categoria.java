package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Categoria de prioridad. Las cuatro categorias base (ids 1 a 4) son:
// Discapacidad, Embarazada y Adulto mayor (preferente = true, mismo nivel entre si)
// y Regular (preferente = false).
// Esas cuatro NO se eliminan, NO se desactivan y su atributo preferente es inmutable:
// los protegen los triggers del SQL y tambien la API (409 Conflict).
// Si se renombra una categoria, cada turno conserva su nombre historico en
// turnos.categoria_nombre_snapshot.
@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    // true para las 3 preferentes base; false para Regular. Inmutable en las base.
    @Column(name = "preferente", nullable = false)
    private Boolean preferente;

    // Las cuatro categorias base permanecen siempre activo = true
    @Column(name = "activo", nullable = false)
    private Boolean activo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Boolean getPreferente() {
        return preferente;
    }

    public void setPreferente(Boolean preferente) {
        this.preferente = preferente;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }
}
