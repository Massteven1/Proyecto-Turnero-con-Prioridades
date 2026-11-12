package com.banco.app.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Registro de turnos emitidos. Corazon del sistema.
// - modulo_id y operador_id son NULL mientras el turno esta EN_ESPERA;
//   se asignan juntos al llamar y se CONSERVAN en estados terminales
//   para mantener la trazabilidad de la asignacion original.
// - categoria_nombre_snapshot guarda el nombre de la categoria AL EMITIRSE el turno:
//   si luego se renombra la categoria, el turno conserva su denominacion historica.
// - modulo_activo_id y operador_activo_id son columnas GENERADAS por MariaDB
//   (SOLO LECTURA): valen el id solo cuando el estado es LLAMADO/EN_ATENCION y
//   sostienen los UNIQUE uk_turnos_modulo_activo y uk_turnos_operador_activo
//   que impiden fisicamente dobles turnos activos. Nunca se escriben desde Java.
@Entity
@Table(name = "turnos")
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unico: PREFIJO-YYYYMMDD-NNN, ej. CAJ-20261105-001 (generado por el backend)
    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    // Ids que se escriben (estilo del proyecto de referencia: escribir el id,
    // leer la relacion para armar los DTO)
    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    // Inmutable tras emitir: trigger trg_turnos_snapshot_inmutable lo protege en SQL
    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    // Nombre historico de la categoria al emitirse ("Discapacidad", "Regular", ...)
    @Column(name = "categoria_nombre_snapshot", nullable = false)
    private String categoriaNombreSnapshot;

    // Nulos hasta el llamado; a partir de LLAMADO quedan fijos para el historial
    @Column(name = "modulo_id")
    private Long moduloId;

    @Column(name = "operador_id")
    private Long operadorId;

    // Dia de operacion America/Bogota (se toma de la unica marca temporal de la transaccion)
    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    // Consecutivo diario por servicio (> 0), unicidad por
    // uk_turnos_servicio_fecha_consecutivo (servicio_id, fecha_operacion, consecutivo)
    @Column(name = "consecutivo", nullable = false)
    private Integer consecutivo;

    // Identificador minimo del cliente (3 a 100 caracteres: documento, nombre corto
    // o ambos). SOLO sale en endpoints protegidos: la pantalla publica nunca lo envia.
    @Column(name = "identificador_cliente", nullable = false)
    private String identificadorCliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTurno estado;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_llamado")
    private LocalDateTime fechaLlamado;

    @Column(name = "fecha_inicio_atencion")
    private LocalDateTime fechaInicioAtencion;

    @Column(name = "fecha_fin_atencion")
    private LocalDateTime fechaFinAtencion;

    // Una sola columna para cancelacion y ausencia (asi lo define el esquema SQL)
    @Column(name = "fecha_cancelacion_ausencia")
    private LocalDateTime fechaCancelacionAusencia;

    // Motivo obligatorio (5-255) cuando el estado es CANCELADO o AUSENTE
    @Column(name = "motivo_cancelacion_ausencia")
    private String motivoCancelacionAusencia;

    // ---- Columnas generadas por MariaDB: SOLO LECTURA, nunca se escriben ----
    @Column(name = "modulo_activo_id", insertable = false, updatable = false)
    private Long moduloActivoId;

    @Column(name = "operador_activo_id", insertable = false, updatable = false)
    private Long operadorActivoId;

    // ---- Relaciones solo lectura para armar los DTO con nombres ----
    @ManyToOne
    @JoinColumn(name = "servicio_id", insertable = false, updatable = false)
    private Servicio servicio;

    @ManyToOne
    @JoinColumn(name = "categoria_id", insertable = false, updatable = false)
    private Categoria categoria;

    @ManyToOne
    @JoinColumn(name = "modulo_id", insertable = false, updatable = false)
    private ModuloAtencion modulo;

    @ManyToOne
    @JoinColumn(name = "operador_id", insertable = false, updatable = false)
    private Usuario operador;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public String getCategoriaNombreSnapshot() {
        return categoriaNombreSnapshot;
    }

    public void setCategoriaNombreSnapshot(String categoriaNombreSnapshot) {
        this.categoriaNombreSnapshot = categoriaNombreSnapshot;
    }

    public Long getModuloId() {
        return moduloId;
    }

    public void setModuloId(Long moduloId) {
        this.moduloId = moduloId;
    }

    public Long getOperadorId() {
        return operadorId;
    }

    public void setOperadorId(Long operadorId) {
        this.operadorId = operadorId;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public void setFechaOperacion(LocalDate fechaOperacion) {
        this.fechaOperacion = fechaOperacion;
    }

    public Integer getConsecutivo() {
        return consecutivo;
    }

    public void setConsecutivo(Integer consecutivo) {
        this.consecutivo = consecutivo;
    }

    public String getIdentificadorCliente() {
        return identificadorCliente;
    }

    public void setIdentificadorCliente(String identificadorCliente) {
        this.identificadorCliente = identificadorCliente;
    }

    public EstadoTurno getEstado() {
        return estado;
    }

    public void setEstado(EstadoTurno estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaIngreso() {
        return fechaIngreso;
    }

    public void setFechaIngreso(LocalDateTime fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public LocalDateTime getFechaLlamado() {
        return fechaLlamado;
    }

    public void setFechaLlamado(LocalDateTime fechaLlamado) {
        this.fechaLlamado = fechaLlamado;
    }

    public LocalDateTime getFechaInicioAtencion() {
        return fechaInicioAtencion;
    }

    public void setFechaInicioAtencion(LocalDateTime fechaInicioAtencion) {
        this.fechaInicioAtencion = fechaInicioAtencion;
    }

    public LocalDateTime getFechaFinAtencion() {
        return fechaFinAtencion;
    }

    public void setFechaFinAtencion(LocalDateTime fechaFinAtencion) {
        this.fechaFinAtencion = fechaFinAtencion;
    }

    public LocalDateTime getFechaCancelacionAusencia() {
        return fechaCancelacionAusencia;
    }

    public void setFechaCancelacionAusencia(LocalDateTime fechaCancelacionAusencia) {
        this.fechaCancelacionAusencia = fechaCancelacionAusencia;
    }

    public String getMotivoCancelacionAusencia() {
        return motivoCancelacionAusencia;
    }

    public void setMotivoCancelacionAusencia(String motivoCancelacionAusencia) {
        this.motivoCancelacionAusencia = motivoCancelacionAusencia;
    }

    public Long getModuloActivoId() {
        return moduloActivoId;
    }

    public Long getOperadorActivoId() {
        return operadorActivoId;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public ModuloAtencion getModulo() {
        return modulo;
    }

    public Usuario getOperador() {
        return operador;
    }
}
