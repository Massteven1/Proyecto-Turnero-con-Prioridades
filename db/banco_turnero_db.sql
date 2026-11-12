-- ============================================================================
-- SISTEMA DE TURNOS BANCARIO CON PRIORIDADES
-- Script de creación de esquema y datos semilla para MySQL / phpMyAdmin
-- Requisito: MySQL 8.0.16 o superior (por uso de restricciones CHECK).
-- La inmutabilidad de categorías base se protege mediante triggers, no CHECK,
-- porque MySQL no permite usar AUTO_INCREMENT dentro de una expresión CHECK.
--
-- IMPORTANTE:
-- 1. El script crea la base de datos `banco_turnero_db` si todavía no existe.
-- 2. Está pensado para ejecutarse una sola vez sobre una base de datos vacía.
--    No elimina tablas ni datos existentes. Si ya existe una instalación previa,
--    respáldala y prepara una base vacía antes de ejecutar este archivo.
-- 3. Las marcas de tiempo de demostración se generan relativamente a la hora
--    actual de la sesión configurada en UTC-05:00 (America/Bogota).
-- 4. La aplicación debe seguir generando y validando sus marcas temporales desde
--    Java usando ZoneId.of("America/Bogota"). DATETIME no almacena zona horaria.
--
-- AJUSTE DE PERSISTENCIA DOCUMENTADO EN ESTE SCRIPT (NO SE MODIFICA EL DOCX):
-- La documentación define `categoriaOriginalNombre` en los DTO como el nombre
-- que tenía la categoría al emitir el turno, incluso si el catálogo se renombra.
-- Sin embargo, la descripción de la tabla `turnos` y el diagrama de clases no
-- enumeran una columna física para guardar esa copia histórica. Por ello este
-- script agrega `categoria_nombre_snapshot VARCHAR(50) NOT NULL` a `turnos`.
-- El backend debe copiar `categorias.nombre` a esta columna al emitir el turno
-- y no volver a modificarla. Los DTO deben leer `categoriaOriginalNombre` desde
-- esta columna, no desde el nombre actual del catálogo.
-- Este campo adicional es necesario para cumplir el comportamiento ya descrito
-- en la documentación sin cambiar sus archivos.
--
-- NOTA SOBRE CONCURRENCIA:
-- Las columnas generadas `modulo_activo_id` y `operador_activo_id` son auxiliares
-- de la base de datos. Sus índices UNIQUE impiden que un módulo o una cuenta
-- tengan dos turnos simultáneos en LLAMADO / EN_ATENCION. El backend igualmente
-- debe aplicar las transacciones, bloqueos y revalidaciones descritos en el DOCX.
--
-- REGLAS QUE SIGUEN SIENDO RESPONSABILIDAD DE SPRING BOOT:
-- * Crear la fila de estado_cola_servicio en la misma transacción que cada servicio.
-- * Ejecutar el rollover diario, numeración, FIFO, envejecimiento y proporción N:1.
-- * Respetar el orden de bloqueos y revalidar cuenta, servicio, módulo y cola.
-- * Validar permisos JWT, propiedad del turno y protección del último ADMIN activo.
-- * Ejecutar exclusivamente las transiciones autorizadas y escribir historial.
-- * Copiar categorias.nombre a categoria_nombre_snapshot al emitir el turno.
-- Los CHECK, índices UNIQUE, claves foráneas y triggers son defensas adicionales;
-- no sustituyen la lógica de negocio ni las transacciones de la capa service.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS banco_turnero_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE banco_turnero_db;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET SESSION time_zone = '-05:00';
SET SESSION sql_mode = 'STRICT_TRANS_TABLES,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- Captura única para que todos los turnos semilla pertenezcan al mismo día.
SET @ahora_demo = CURRENT_TIMESTAMP(6);
SET @fecha_demo = DATE(@ahora_demo);

-- ============================================================================
-- 1. USUARIOS
-- Roles autenticados permitidos: ADMIN y OPERADOR.
-- Consulta pública es anónima y NO se almacena como usuario ni como rol.
-- ============================================================================

CREATE TABLE usuarios (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    username         VARCHAR(50) NOT NULL,
    nombre_completo  VARCHAR(100) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    rol              VARCHAR(20) NOT NULL,
    activo           TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT chk_usuarios_rol CHECK (rol IN ('ADMIN', 'OPERADOR')),
    CONSTRAINT chk_usuarios_activo CHECK (activo IN (0, 1)),
    CONSTRAINT chk_usuarios_username CHECK (CHAR_LENGTH(TRIM(username)) BETWEEN 3 AND 50),
    CONSTRAINT chk_usuarios_nombre CHECK (CHAR_LENGTH(TRIM(nombre_completo)) BETWEEN 2 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. SERVICIOS
-- codigo_prefijo identifica el servicio en el código visible del turno.
-- El prefijo no se debe modificar después de crear el servicio (regla del API).
-- ============================================================================

CREATE TABLE servicios (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    codigo_prefijo VARCHAR(10) NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    descripcion    VARCHAR(255) NULL,
    activo         TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_servicios PRIMARY KEY (id),
    CONSTRAINT uk_servicios_codigo_prefijo UNIQUE (codigo_prefijo),
    CONSTRAINT uk_servicios_nombre UNIQUE (nombre),
    CONSTRAINT chk_servicios_activo CHECK (activo IN (0, 1)),
    CONSTRAINT chk_servicios_prefijo CHECK (CHAR_LENGTH(TRIM(codigo_prefijo)) BETWEEN 2 AND 10),
    CONSTRAINT chk_servicios_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) BETWEEN 3 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. MÓDULOS DE ATENCIÓN
-- El nombre es único globalmente, como establece la especificación.
-- UNIQUE (id, servicio_id) permite la FK compuesta desde turnos y evita que un
-- turno sea asignado a un módulo perteneciente a otro servicio.
-- ============================================================================

CREATE TABLE modulos_atencion (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    servicio_id BIGINT NOT NULL,
    nombre      VARCHAR(50) NOT NULL,
    activo      TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_modulos_atencion PRIMARY KEY (id),
    CONSTRAINT uk_modulos_nombre UNIQUE (nombre),
    CONSTRAINT uk_modulos_id_servicio UNIQUE (id, servicio_id),
    INDEX idx_modulos_servicio_activo (servicio_id, activo),
    CONSTRAINT fk_modulos_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_modulos_activo CHECK (activo IN (0, 1)),
    CONSTRAINT chk_modulos_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) BETWEEN 3 AND 50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. CATEGORÍAS
-- Los IDs 1 a 4 identifican las cuatro categorías base. Pueden renombrarse,
-- pero no desactivarse ni cambiar su clasificación preferente.
-- Los turnos guardan además su propio snapshot de nombre histórico.
-- ============================================================================

CREATE TABLE categorias (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    nombre     VARCHAR(50) NOT NULL,
    preferente TINYINT(1) NOT NULL DEFAULT 0,
    activo     TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT uk_categorias_nombre UNIQUE (nombre),
    CONSTRAINT chk_categorias_booleanos CHECK (preferente IN (0, 1) AND activo IN (0, 1)),
    CONSTRAINT chk_categorias_nombre CHECK (CHAR_LENGTH(TRIM(nombre)) BETWEEN 3 AND 50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. ESTADO DE COLA POR SERVICIO
-- Una fila por servicio. Se reutiliza y actualiza cuando cambia el día de
-- operación; no se crea una fila nueva cada medianoche.
-- ============================================================================

CREATE TABLE estado_cola_servicio (
    id                       BIGINT NOT NULL AUTO_INCREMENT,
    servicio_id              BIGINT NOT NULL,
    fecha_operacion          DATE NOT NULL,
    ultimo_consecutivo       INT NOT NULL DEFAULT 0,
    contador_preferentes     INT NOT NULL DEFAULT 0,
    umbral_envejecimiento_min INT NOT NULL DEFAULT 15,
    proporcion_preferente   INT NOT NULL DEFAULT 3,

    CONSTRAINT pk_estado_cola_servicio PRIMARY KEY (id),
    CONSTRAINT uk_estado_cola_servicio UNIQUE (servicio_id),
    CONSTRAINT fk_estado_cola_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_estado_cola_ultimo_consecutivo CHECK (ultimo_consecutivo >= 0),
    CONSTRAINT chk_estado_cola_umbral CHECK (umbral_envejecimiento_min BETWEEN 1 AND 240),
    CONSTRAINT chk_estado_cola_proporcion CHECK (proporcion_preferente BETWEEN 1 AND 10),
    CONSTRAINT chk_estado_cola_contador CHECK (
        contador_preferentes >= 0 AND contador_preferentes <= proporcion_preferente
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. TURNOS
-- `operador_id` y `modulo_id` permanecen NULL mientras el turno está EN_ESPERA.
-- Al ser llamado se asignan ambos y se conservan después de estados terminales
-- para mantener la trazabilidad de la asignación original.
--
-- `categoria_nombre_snapshot` es la columna adicional explicada al inicio.
-- `modulo_activo_id` y `operador_activo_id` son columnas generadas internas:
-- valen NULL para estados no activos, por lo que sus índices UNIQUE permiten
-- muchos turnos históricos pero solo uno activo por módulo/cuenta.
-- ============================================================================

CREATE TABLE turnos (
    id                         BIGINT NOT NULL AUTO_INCREMENT,
    codigo                     VARCHAR(30) NOT NULL,
    servicio_id                BIGINT NOT NULL,
    categoria_id               BIGINT NOT NULL,
    categoria_nombre_snapshot  VARCHAR(50) NOT NULL,
    modulo_id                  BIGINT NULL,
    operador_id                BIGINT NULL,
    fecha_operacion            DATE NOT NULL,
    consecutivo                INT NOT NULL,
    identificador_cliente      VARCHAR(100) NOT NULL,
    estado                     VARCHAR(20) NOT NULL DEFAULT 'EN_ESPERA',
    fecha_ingreso              DATETIME(6) NOT NULL,
    fecha_llamado              DATETIME(6) NULL,
    fecha_inicio_atencion      DATETIME(6) NULL,
    fecha_fin_atencion         DATETIME(6) NULL,
    fecha_cancelacion_ausencia DATETIME(6) NULL,
    motivo_cancelacion_ausencia VARCHAR(255) NULL,

    -- Columnas generadas para imponer exclusividad también a nivel SQL.
    modulo_activo_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN estado IN ('LLAMADO', 'EN_ATENCION') THEN modulo_id ELSE NULL END
    ) STORED,
    operador_activo_id BIGINT GENERATED ALWAYS AS (
        CASE WHEN estado IN ('LLAMADO', 'EN_ATENCION') THEN operador_id ELSE NULL END
    ) STORED,

    CONSTRAINT pk_turnos PRIMARY KEY (id),
    CONSTRAINT uk_turnos_codigo UNIQUE (codigo),
    CONSTRAINT uk_turnos_servicio_fecha_consecutivo
        UNIQUE (servicio_id, fecha_operacion, consecutivo),
    CONSTRAINT uk_turnos_modulo_activo UNIQUE (modulo_activo_id),
    CONSTRAINT uk_turnos_operador_activo UNIQUE (operador_activo_id),

    INDEX idx_turnos_cola_espera (servicio_id, fecha_operacion, estado, fecha_ingreso, id),
    INDEX idx_turnos_modulo_estado (modulo_id, estado),
    INDEX idx_turnos_operador_estado (operador_id, estado),
    INDEX idx_turnos_historial_filtros (fecha_operacion, servicio_id, estado),
    INDEX idx_turnos_categoria (categoria_id),

    CONSTRAINT fk_turnos_servicio
        FOREIGN KEY (servicio_id) REFERENCES servicios (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_turnos_categoria
        FOREIGN KEY (categoria_id) REFERENCES categorias (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_turnos_modulo_servicio
        FOREIGN KEY (modulo_id, servicio_id)
        REFERENCES modulos_atencion (id, servicio_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_turnos_operador
        FOREIGN KEY (operador_id) REFERENCES usuarios (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,

    CONSTRAINT chk_turnos_consecutivo CHECK (consecutivo > 0),
    CONSTRAINT chk_turnos_estado CHECK (
        estado IN ('EN_ESPERA', 'LLAMADO', 'EN_ATENCION', 'FINALIZADO', 'CANCELADO', 'AUSENTE')
    ),
    CONSTRAINT chk_turnos_identificador CHECK (
        CHAR_LENGTH(TRIM(identificador_cliente)) BETWEEN 3 AND 100
    ),
    CONSTRAINT chk_turnos_categoria_snapshot CHECK (
        CHAR_LENGTH(TRIM(categoria_nombre_snapshot)) BETWEEN 3 AND 50
    ),
    CONSTRAINT chk_turnos_asignacion_pareada CHECK (
        (modulo_id IS NULL AND operador_id IS NULL) OR
        (modulo_id IS NOT NULL AND operador_id IS NOT NULL)
    ),
    CONSTRAINT chk_turnos_espera_sin_asignacion CHECK (
        estado <> 'EN_ESPERA' OR (modulo_id IS NULL AND operador_id IS NULL)
    ),
    CONSTRAINT chk_turnos_estados_asignados CHECK (
        estado NOT IN ('LLAMADO', 'EN_ATENCION', 'FINALIZADO', 'AUSENTE') OR
        (modulo_id IS NOT NULL AND operador_id IS NOT NULL)
    ),
    CONSTRAINT chk_turnos_cancelado_asignacion CHECK (
        estado <> 'CANCELADO' OR
        (fecha_llamado IS NULL AND modulo_id IS NULL AND operador_id IS NULL) OR
        (fecha_llamado IS NOT NULL AND modulo_id IS NOT NULL AND operador_id IS NOT NULL)
    ),
    CONSTRAINT chk_turnos_motivo_salida CHECK (
        (
            estado IN ('CANCELADO', 'AUSENTE')
            AND fecha_cancelacion_ausencia IS NOT NULL
            AND motivo_cancelacion_ausencia IS NOT NULL
            AND CHAR_LENGTH(TRIM(motivo_cancelacion_ausencia)) BETWEEN 5 AND 255
        ) OR (
            estado NOT IN ('CANCELADO', 'AUSENTE')
            AND fecha_cancelacion_ausencia IS NULL
            AND motivo_cancelacion_ausencia IS NULL
        )
    ),
    CONSTRAINT chk_turnos_fechas_estado CHECK (
        (estado = 'EN_ESPERA'
            AND fecha_llamado IS NULL
            AND fecha_inicio_atencion IS NULL
            AND fecha_fin_atencion IS NULL)
        OR
        (estado = 'LLAMADO'
            AND fecha_llamado IS NOT NULL
            AND fecha_inicio_atencion IS NULL
            AND fecha_fin_atencion IS NULL)
        OR
        (estado = 'EN_ATENCION'
            AND fecha_llamado IS NOT NULL
            AND fecha_inicio_atencion IS NOT NULL
            AND fecha_fin_atencion IS NULL)
        OR
        (estado = 'FINALIZADO'
            AND fecha_llamado IS NOT NULL
            AND fecha_inicio_atencion IS NOT NULL
            AND fecha_fin_atencion IS NOT NULL)
        OR
        (estado = 'CANCELADO'
            AND fecha_inicio_atencion IS NULL
            AND fecha_fin_atencion IS NULL)
        OR
        (estado = 'AUSENTE'
            AND fecha_llamado IS NOT NULL
            AND fecha_inicio_atencion IS NULL
            AND fecha_fin_atencion IS NULL)
    ),
    CONSTRAINT chk_turnos_orden_temporal CHECK (
        (fecha_llamado IS NULL OR fecha_llamado >= fecha_ingreso)
        AND (fecha_inicio_atencion IS NULL OR
             (fecha_llamado IS NOT NULL AND fecha_inicio_atencion >= fecha_llamado))
        AND (fecha_fin_atencion IS NULL OR
             (fecha_inicio_atencion IS NOT NULL AND fecha_fin_atencion >= fecha_inicio_atencion))
        AND (fecha_cancelacion_ausencia IS NULL OR
             fecha_cancelacion_ausencia >= COALESCE(fecha_llamado, fecha_ingreso))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 7. HISTORIAL DE TURNOS
-- Cada emisión y cada transición se registra con el usuario autenticado que
-- ejecutó la operación. estado_anterior es NULL solo en la emisión inicial.
-- ============================================================================

CREATE TABLE historial_turnos (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    turno_id        BIGINT NOT NULL,
    operador_id     BIGINT NOT NULL,
    estado_anterior VARCHAR(20) NULL,
    estado_nuevo    VARCHAR(20) NOT NULL,
    fecha_cambio    DATETIME(6) NOT NULL,
    motivo          VARCHAR(255) NULL,

    CONSTRAINT pk_historial_turnos PRIMARY KEY (id),
    INDEX idx_historial_turno_fecha (turno_id, fecha_cambio, id),
    INDEX idx_historial_operador (operador_id),
    CONSTRAINT fk_historial_turno
        FOREIGN KEY (turno_id) REFERENCES turnos (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_historial_operador
        FOREIGN KEY (operador_id) REFERENCES usuarios (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_historial_estado_anterior CHECK (
        estado_anterior IS NULL OR estado_anterior IN
        ('EN_ESPERA', 'LLAMADO', 'EN_ATENCION', 'FINALIZADO', 'CANCELADO', 'AUSENTE')
    ),
    CONSTRAINT chk_historial_estado_nuevo CHECK (
        estado_nuevo IN
        ('EN_ESPERA', 'LLAMADO', 'EN_ATENCION', 'FINALIZADO', 'CANCELADO', 'AUSENTE')
    ),
    CONSTRAINT chk_historial_transicion CHECK (
        (estado_anterior IS NULL AND estado_nuevo = 'EN_ESPERA') OR
        (estado_anterior IS NOT NULL AND (
            (estado_anterior = 'EN_ESPERA' AND estado_nuevo IN ('LLAMADO', 'CANCELADO')) OR
            (estado_anterior = 'LLAMADO' AND estado_nuevo IN ('EN_ATENCION', 'CANCELADO', 'AUSENTE')) OR
            (estado_anterior = 'EN_ATENCION' AND estado_nuevo = 'FINALIZADO')
        ))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- TRIGGERS DE PROTECCIÓN DE DATOS HISTÓRICOS
-- La API también debe aplicar estas reglas. Los triggers añaden defensa ante
-- modificaciones accidentales directas desde SQL.
-- ============================================================================

DELIMITER $$

-- MySQL no permite referenciar una columna AUTO_INCREMENT dentro de un CHECK
-- (error #1901). Por eso la inmutabilidad de las cuatro categorías base se
-- implementa con triggers. Se permite cambiar `nombre`, pero no el ID, `preferente`
-- ni `activo` de las categorías base (IDs 1 a 4).
CREATE TRIGGER trg_categorias_base_no_update_id
BEFORE UPDATE ON categorias
FOR EACH ROW
BEGIN
    IF OLD.id IN (1, 2, 3, 4) AND (
        NEW.id <> OLD.id
        OR NEW.activo <> 1
        OR (OLD.id IN (1, 2, 3) AND NEW.preferente <> 1)
        OR (OLD.id = 4 AND NEW.preferente <> 0)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las categorias base no permiten cambiar ID, preferente ni activo';
    END IF;
END$$

CREATE TRIGGER trg_categorias_base_no_delete
BEFORE DELETE ON categorias
FOR EACH ROW
BEGIN
    IF OLD.id IN (1, 2, 3, 4) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Las cuatro categorias base no se pueden eliminar';
    END IF;
END$$

CREATE TRIGGER trg_historial_turnos_no_update
BEFORE UPDATE ON historial_turnos
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El historial de turnos es inmutable: no se permiten UPDATE';
END$$

CREATE TRIGGER trg_historial_turnos_no_delete
BEFORE DELETE ON historial_turnos
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'El historial de turnos es inmutable: no se permiten DELETE';
END$$

CREATE TRIGGER trg_turnos_snapshot_inmutable
BEFORE UPDATE ON turnos
FOR EACH ROW
BEGIN
    IF NOT (NEW.categoria_id <=> OLD.categoria_id)
       OR NOT (NEW.categoria_nombre_snapshot <=> OLD.categoria_nombre_snapshot) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La categoria original del turno y su nombre historico no se pueden modificar';
    END IF;
END$$

DELIMITER ;

-- ============================================================================
-- DATOS SEMILLA
-- Contraseñas de demostración (cambiarlas antes de cualquier uso real):
-- admin       / Admin123!
-- operador1   / Operador123!
-- operador2   / Operador456!
-- Los hashes son BCrypt con coste 10. El prefijo $2a$ es compatible con
-- Spring Security BCryptPasswordEncoder.
-- ============================================================================

START TRANSACTION;

INSERT INTO usuarios (id, username, nombre_completo, password_hash, rol, activo) VALUES
(1, 'admin',     'Administrador General Banco', '$2a$10$O4u5O1Q2ZXrle0o6Jao5eumBsRbNWH3WYTDBrmx0Sl3YdibCG92XG', 'ADMIN',    1),
(2, 'operador1', 'Carlos Perez - Cajero 1',      '$2a$10$Z5uILyN..J8T7X.GU.IFtuIhllHdBHmC7XQPA1WmtNsdRLzBZRC2G', 'OPERADOR', 1),
(3, 'operador2', 'Laura Gomez - Cajero 2',       '$2a$10$ShkxxGv62W78z5d.MT96j.3spqYoSbDfWOlgvyguEP4e7mTvRhXTS', 'OPERADOR', 1);

INSERT INTO servicios (id, codigo_prefijo, nombre, descripcion, activo) VALUES
(1, 'CAJ', 'Caja General', 'Depositos, retiros y pagos en ventanilla', 1),
(2, 'ASE', 'Asesoria Comercial', 'Apertura de productos, creditos y certificados', 1);

INSERT INTO modulos_atencion (id, servicio_id, nombre, activo) VALUES
(1, 1, 'Ventanilla Caja 1', 1),
(2, 1, 'Ventanilla Caja 2', 1),
(3, 2, 'Modulo Asesoria 1', 1);

INSERT INTO categorias (id, nombre, preferente, activo) VALUES
(1, 'Discapacidad', 1, 1),
(2, 'Embarazada',   1, 1),
(3, 'Adulto mayor', 1, 1),
(4, 'Regular',      0, 1);

-- Los contadores de ambos servicios quedan en 0 porque su último llamado
-- semilla fue un turno Regular no envejecido (CAJ-...-002 y ASE-...-001).
INSERT INTO estado_cola_servicio
    (id, servicio_id, fecha_operacion, ultimo_consecutivo,
     contador_preferentes, umbral_envejecimiento_min, proporcion_preferente)
VALUES
(1, 1, @fecha_demo, 10, 0, 15, 3),
(2, 2, @fecha_demo,  2, 0, 15, 3);

-- Estado activo inicial para probar sin bloquear toda la operación:
-- CAJ-...-001 está LLAMADO en Caja / Ventanilla 1 por operador1.
-- ASE-...-001 está EN_ATENCION en Asesoría / Módulo 1 por operador2.
-- ADMIN no tiene un turno activo y puede probar el siguiente llamado en Caja /
-- Ventanilla 2. Las cuentas que ya tienen un turno activo deben resolverlo antes
-- de solicitar otro, según la regla de exclusividad por cuenta.
--
-- Turnos de Caja General:
-- 001 LLAMADO, asignado a operador1 / Ventanilla Caja 1.
-- 002 FINALIZADO, asignado históricamente a operador2 / Ventanilla Caja 2.
-- 003 FINALIZADO.
-- 004 CANCELADO desde EN_ESPERA (sin asignación de módulo ni operador en turnos).
-- 005 AUSENTE desde LLAMADO.
-- 006 Regular envejecido (> 15 minutos), ingresó antes que los preferentes.
-- 007-009 categorías preferentes; 010 Regular no envejecido.
INSERT INTO turnos (
    id, codigo, servicio_id, categoria_id, categoria_nombre_snapshot,
    modulo_id, operador_id, fecha_operacion, consecutivo,
    identificador_cliente, estado, fecha_ingreso, fecha_llamado,
    fecha_inicio_atencion, fecha_fin_atencion,
    fecha_cancelacion_ausencia, motivo_cancelacion_ausencia
) VALUES
(1, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-001'),
 1, 1, 'Discapacidad', 1, 2, @fecha_demo, 1,
 'CC 1088101 - Carlos M.', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 45 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 10 MINUTE), NULL, NULL, NULL, NULL),

(2, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-002'),
 1, 4, 'Regular', 2, 3, @fecha_demo, 2,
 'CC 1088102 - Laura G.', 'FINALIZADO',
 DATE_SUB(@ahora_demo, INTERVAL 8 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 6 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 3 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 1 MINUTE), NULL, NULL),

(3, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-003'),
 1, 2, 'Embarazada', 1, 2, @fecha_demo, 3,
 'CC 1088103 - Maria R.', 'FINALIZADO',
 DATE_SUB(@ahora_demo, INTERVAL 120 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 115 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 110 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 100 MINUTE), NULL, NULL),

(4, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-004'),
 1, 3, 'Adulto mayor', NULL, NULL, @fecha_demo, 4,
 'CC 1088104 - Jorge P.', 'CANCELADO',
 DATE_SUB(@ahora_demo, INTERVAL 90 MINUTE),
 NULL, NULL, NULL,
 DATE_SUB(@ahora_demo, INTERVAL 85 MINUTE), 'El cliente solicito cancelar el turno'),

(5, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-005'),
 1, 4, 'Regular', 2, 3, @fecha_demo, 5,
 'CC 1088105 - Diana S.', 'AUSENTE',
 DATE_SUB(@ahora_demo, INTERVAL 75 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 70 MINUTE), NULL, NULL,
 DATE_SUB(@ahora_demo, INTERVAL 65 MINUTE), 'El cliente no se presento en ventanilla'),

(6, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-006'),
 1, 4, 'Regular', NULL, NULL, @fecha_demo, 6,
 'CC 1088106 - Pedro A.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 50 MINUTE), NULL, NULL, NULL, NULL, NULL),

(7, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-007'),
 1, 1, 'Discapacidad', NULL, NULL, @fecha_demo, 7,
 'CC 1088107 - Sofia C.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 30 MINUTE), NULL, NULL, NULL, NULL, NULL),

(8, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-008'),
 1, 2, 'Embarazada', NULL, NULL, @fecha_demo, 8,
 'CC 1088108 - Ana V.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 25 MINUTE), NULL, NULL, NULL, NULL, NULL),

(9, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-009'),
 1, 3, 'Adulto mayor', NULL, NULL, @fecha_demo, 9,
 'CC 1088109 - Luis F.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 20 MINUTE), NULL, NULL, NULL, NULL, NULL),

(10, CONCAT('CAJ-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-010'),
 1, 4, 'Regular', NULL, NULL, @fecha_demo, 10,
 'CC 1088110 - Elena B.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 5 MINUTE), NULL, NULL, NULL, NULL, NULL),

-- En Asesoría, el turno 001 está EN_ATENCION (operador2 / módulo 3) y el
-- turno 002 espera. Caja conserva el módulo 2 y la cuenta ADMIN disponibles
-- para probar nuevos llamados en su cola independiente.
(11, CONCAT('ASE-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-001'),
 2, 4, 'Regular', 3, 3, @fecha_demo, 1,
 'CC 2088101 - Mateo L.', 'EN_ATENCION',
 DATE_SUB(@ahora_demo, INTERVAL 12 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 10 MINUTE),
 DATE_SUB(@ahora_demo, INTERVAL 8 MINUTE), NULL, NULL, NULL),

(12, CONCAT('ASE-', DATE_FORMAT(@fecha_demo, '%Y%m%d'), '-002'),
 2, 4, 'Regular', NULL, NULL, @fecha_demo, 2,
 'CC 2088102 - Natalia D.', 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 4 MINUTE), NULL, NULL, NULL, NULL, NULL);

-- Historial cronológico completo para cada turno semilla.
-- operador_id identifica al usuario que ejecutó cada operación, incluso en la
-- emisión inicial; no debe confundirse con turnos.operador_id, que permanece
-- NULL mientras el turno está EN_ESPERA y luego conserva al operador asignado.
INSERT INTO historial_turnos
    (id, turno_id, operador_id, estado_anterior, estado_nuevo, fecha_cambio, motivo)
VALUES
-- Turno 001 de Caja: emisión y llamado (permanece LLAMADO).
(1, 1, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 45 MINUTE), 'Emision inicial del turno'),
(2, 1, 2, 'EN_ESPERA', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 10 MINUTE), 'Llamado en Ventanilla Caja 1'),

-- Turno 002 de Caja: emisión, llamado, inicio y finalización.
(3, 2, 3, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 8 MINUTE), 'Emision inicial del turno'),
(4, 2, 3, 'EN_ESPERA', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 6 MINUTE), 'Llamado en Ventanilla Caja 2'),
(5, 2, 3, 'LLAMADO', 'EN_ATENCION',
 DATE_SUB(@ahora_demo, INTERVAL 3 MINUTE), 'Inicio de atencion'),
(6, 2, 3, 'EN_ATENCION', 'FINALIZADO',
 DATE_SUB(@ahora_demo, INTERVAL 1 MINUTE), 'Atencion finalizada'),

-- Turno 003 de Caja: flujo completo hasta FINALIZADO.
(7, 3, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 120 MINUTE), 'Emision inicial del turno'),
(8, 3, 2, 'EN_ESPERA', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 115 MINUTE), 'Llamado en Ventanilla Caja 1'),
(9, 3, 2, 'LLAMADO', 'EN_ATENCION',
 DATE_SUB(@ahora_demo, INTERVAL 110 MINUTE), 'Inicio de atencion'),
(10, 3, 2, 'EN_ATENCION', 'FINALIZADO',
 DATE_SUB(@ahora_demo, INTERVAL 100 MINUTE), 'Atencion finalizada'),

-- Turno 004 de Caja: cancelado directamente desde EN_ESPERA.
(11, 4, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 90 MINUTE), 'Emision inicial del turno'),
(12, 4, 2, 'EN_ESPERA', 'CANCELADO',
 DATE_SUB(@ahora_demo, INTERVAL 85 MINUTE), 'El cliente solicito cancelar el turno'),

-- Turno 005 de Caja: llamado y posteriormente declarado AUSENTE.
(13, 5, 3, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 75 MINUTE), 'Emision inicial del turno'),
(14, 5, 3, 'EN_ESPERA', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 70 MINUTE), 'Llamado en Ventanilla Caja 2'),
(15, 5, 3, 'LLAMADO', 'AUSENTE',
 DATE_SUB(@ahora_demo, INTERVAL 65 MINUTE), 'El cliente no se presento en ventanilla'),

-- Turnos 006 a 010 de Caja: permanecen en espera; 006 está envejecido.
(16, 6, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 50 MINUTE), 'Emision inicial del turno'),
(17, 7, 3, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 30 MINUTE), 'Emision inicial del turno'),
(18, 8, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 25 MINUTE), 'Emision inicial del turno'),
(19, 9, 3, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 20 MINUTE), 'Emision inicial del turno'),
(20, 10, 2, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 5 MINUTE), 'Emision inicial del turno'),

-- Asesoría: turno 001 en atención y turno 002 todavía en espera.
(21, 11, 3, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 12 MINUTE), 'Emision inicial del turno'),
(22, 11, 3, 'EN_ESPERA', 'LLAMADO',
 DATE_SUB(@ahora_demo, INTERVAL 10 MINUTE), 'Llamado en Modulo Asesoria 1'),
(23, 11, 3, 'LLAMADO', 'EN_ATENCION',
 DATE_SUB(@ahora_demo, INTERVAL 8 MINUTE), 'Inicio de atencion'),
(24, 12, 1, NULL, 'EN_ESPERA',
 DATE_SUB(@ahora_demo, INTERVAL 4 MINUTE), 'Emision inicial del turno');

COMMIT;

-- ============================================================================
-- COMPROBACIONES RÁPIDAS
-- Estas consultas permiten verificar el contenido después de ejecutar el script.
-- ============================================================================

SELECT 'usuarios' AS tabla, COUNT(*) AS registros FROM usuarios
UNION ALL SELECT 'servicios', COUNT(*) FROM servicios
UNION ALL SELECT 'modulos_atencion', COUNT(*) FROM modulos_atencion
UNION ALL SELECT 'categorias', COUNT(*) FROM categorias
UNION ALL SELECT 'estado_cola_servicio', COUNT(*) FROM estado_cola_servicio
UNION ALL SELECT 'turnos', COUNT(*) FROM turnos
UNION ALL SELECT 'historial_turnos', COUNT(*) FROM historial_turnos;

-- Debería devolver el turno CAJ-...-006 como Regular envejecido (>= 15 min).
SELECT
    codigo,
    categoria_nombre_snapshot AS categoria_original,
    estado,
    TIMESTAMPDIFF(MINUTE, fecha_ingreso, @ahora_demo) AS espera_minutos
FROM turnos
WHERE servicio_id = 1
  AND estado = 'EN_ESPERA'
ORDER BY fecha_ingreso ASC, id ASC;

-- Debe devolver los dos turnos activos semilla: uno LLAMADO y otro EN_ATENCION.
SELECT codigo, estado, modulo_id, operador_id
FROM turnos
WHERE estado IN ('LLAMADO', 'EN_ATENCION')
ORDER BY fecha_llamado ASC;

-- Credenciales de demostración:
-- admin       / Admin123!
-- operador1   / Operador123!
-- operador2   / Operador456!
