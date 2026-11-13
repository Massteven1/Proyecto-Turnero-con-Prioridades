package com.banco.app.repository;

// Proyeccion de solo lectura para el listado de modulos: identifica el turno
// activo (LLAMADO o EN_ATENCION) que ocupa cada ventanilla.
public interface TurnoActivoView {

    Long getModuloId();

    String getCodigo();
}