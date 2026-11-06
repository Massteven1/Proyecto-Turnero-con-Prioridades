-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Servidor: 127.0.0.1
-- Tiempo de generación: 06-11-2026 a las 16:55:47
-- Versión del servidor: 10.4.32-MariaDB
-- Versión de PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";
CREATE DATABASE IF NOT EXISTS `banco_turnero_db`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `banco_turnero_db`;


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `banco_turnero_db`
--

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `categorias`
--

CREATE TABLE `categorias` (
  `id` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `preferente` tinyint(1) NOT NULL DEFAULT 0,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `categorias`
--

INSERT INTO `categorias` (`id`, `nombre`, `preferente`, `activo`) VALUES
(1, 'Discapacidad', 1, 1),
(2, 'Embarazada', 1, 1),
(3, 'Adulto mayor', 1, 1),
(4, 'Regular', 0, 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `estado_cola_servicio`
--

CREATE TABLE `estado_cola_servicio` (
  `id` bigint(20) NOT NULL,
  `servicio_id` bigint(20) NOT NULL,
  `fecha_operacion` date NOT NULL,
  `ultimo_consecutivo` int(11) NOT NULL DEFAULT 0,
  `contador_preferentes` int(11) NOT NULL DEFAULT 0,
  `umbral_envejecimiento_min` int(11) NOT NULL DEFAULT 15,
  `proporcion_preferente` int(11) NOT NULL DEFAULT 3
) ;

--
-- Volcado de datos para la tabla `estado_cola_servicio`
--

INSERT INTO `estado_cola_servicio` (`id`, `servicio_id`, `fecha_operacion`, `ultimo_consecutivo`, `contador_preferentes`, `umbral_envejecimiento_min`, `proporcion_preferente`) VALUES
(1, 1, '2026-11-05', 0, 0, 15, 3),
(2, 2, '2026-11-05', 0, 0, 15, 3);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `historial_turnos`
--

CREATE TABLE `historial_turnos` (
  `id` bigint(20) NOT NULL,
  `turno_id` bigint(20) NOT NULL,
  `operador_id` bigint(20) DEFAULT NULL,
  `estado_anterior` varchar(20) DEFAULT NULL,
  `estado_nuevo` varchar(20) NOT NULL,
  `fecha_cambio` datetime NOT NULL,
  `motivo` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `modulos_atencion`
--

CREATE TABLE `modulos_atencion` (
  `id` bigint(20) NOT NULL,
  `servicio_id` bigint(20) NOT NULL,
  `nombre` varchar(50) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `modulos_atencion`
--

INSERT INTO `modulos_atencion` (`id`, `servicio_id`, `nombre`, `activo`) VALUES
(1, 1, 'Ventanilla Caja 1', 1),
(2, 1, 'Ventanilla Caja 2', 1),
(3, 2, 'Módulo Asesoría 1', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `servicios`
--

CREATE TABLE `servicios` (
  `id` bigint(20) NOT NULL,
  `codigo_prefijo` varchar(10) NOT NULL,
  `nombre` varchar(100) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Volcado de datos para la tabla `servicios`
--

INSERT INTO `servicios` (`id`, `codigo_prefijo`, `nombre`, `descripcion`, `activo`) VALUES
(1, 'CAJ', 'Caja General', 'Depósitos, retiros y pagos en ventanilla', 1),
(2, 'ASE', 'Asesoría Comercial', 'Apertura de productos, créditos y certificados', 1);

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `turnos`
--

CREATE TABLE `turnos` (
  `id` bigint(20) NOT NULL,
  `codigo` varchar(30) NOT NULL,
  `servicio_id` bigint(20) NOT NULL,
  `categoria_id` bigint(20) NOT NULL,
  `modulo_id` bigint(20) DEFAULT NULL,
  `operador_id` bigint(20) DEFAULT NULL,
  `fecha_operacion` date NOT NULL,
  `consecutivo` int(11) NOT NULL,
  `identificador_cliente` varchar(100) NOT NULL,
  `estado` varchar(20) NOT NULL DEFAULT 'EN_ESPERA',
  `fecha_ingreso` datetime NOT NULL,
  `fecha_llamado` datetime DEFAULT NULL,
  `fecha_inicio_atencion` datetime DEFAULT NULL,
  `fecha_fin_atencion` datetime DEFAULT NULL,
  `fecha_cancelacion_ausencia` datetime DEFAULT NULL,
  `motivo_cancelacion_ausencia` varchar(255) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Estructura de tabla para la tabla `usuarios`
--

CREATE TABLE `usuarios` (
  `id` bigint(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `nombre_completo` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `rol` varchar(20) NOT NULL,
  `activo` tinyint(1) NOT NULL DEFAULT 1
) ;

--
-- Volcado de datos para la tabla `usuarios`
--

INSERT INTO `usuarios` (`id`, `username`, `nombre_completo`, `password_hash`, `rol`, `activo`) VALUES
(1, 'admin', 'Administrador General Banco', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M1h8r5/4pG6tZ0xV.v3Y9K8W1e2G', 'ADMIN', 1),
(2, 'operador1', 'Carlos Pérez - Cajero 1', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M1h8r5/4pG6tZ0xV.v3Y9K8W1e2G', 'OPERADOR', 1),
(3, 'operador2', 'Laura Gómez - Cajero 2', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M1h8r5/4pG6tZ0xV.v3Y9K8W1e2G', 'OPERADOR', 1);

--
-- Índices para tablas volcadas
--

--
-- Indices de la tabla `categorias`
--
ALTER TABLE `categorias`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_categorias_nombre` (`nombre`);

--
-- Indices de la tabla `estado_cola_servicio`
--
ALTER TABLE `estado_cola_servicio`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_estado_cola_servicio` (`servicio_id`);

--
-- Indices de la tabla `historial_turnos`
--
ALTER TABLE `historial_turnos`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_historial_operador` (`operador_id`),
  ADD KEY `idx_historial_turno_fecha` (`turno_id`,`fecha_cambio`,`id`);

--
-- Indices de la tabla `modulos_atencion`
--
ALTER TABLE `modulos_atencion`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_modulos_nombre` (`nombre`),
  ADD KEY `fk_modulos_servicio` (`servicio_id`);

--
-- Indices de la tabla `servicios`
--
ALTER TABLE `servicios`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_servicios_prefijo` (`codigo_prefijo`),
  ADD UNIQUE KEY `uk_servicios_nombre` (`nombre`);

--
-- Indices de la tabla `turnos`
--
ALTER TABLE `turnos`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_turnos_codigo` (`codigo`),
  ADD UNIQUE KEY `uk_turnos_servicio_fecha_consecutivo` (`servicio_id`,`fecha_operacion`,`consecutivo`),
  ADD KEY `fk_turnos_categoria` (`categoria_id`),
  ADD KEY `fk_turnos_operador` (`operador_id`),
  ADD KEY `idx_turnos_cola_espera` (`servicio_id`,`estado`,`fecha_ingreso`,`id`),
  ADD KEY `idx_turnos_modulo_activo` (`modulo_id`,`estado`),
  ADD KEY `idx_turnos_historial_filtros` (`fecha_operacion`,`servicio_id`,`estado`);

--
-- Indices de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_usuarios_username` (`username`);

--
-- AUTO_INCREMENT de las tablas volcadas
--

--
-- AUTO_INCREMENT de la tabla `categorias`
--
ALTER TABLE `categorias`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT de la tabla `estado_cola_servicio`
--
ALTER TABLE `estado_cola_servicio`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `historial_turnos`
--
ALTER TABLE `historial_turnos`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `modulos_atencion`
--
ALTER TABLE `modulos_atencion`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT de la tabla `servicios`
--
ALTER TABLE `servicios`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT de la tabla `turnos`
--
ALTER TABLE `turnos`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT de la tabla `usuarios`
--
ALTER TABLE `usuarios`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- Restricciones para tablas volcadas
--

--
-- Filtros para la tabla `estado_cola_servicio`
--
ALTER TABLE `estado_cola_servicio`
  ADD CONSTRAINT `fk_estado_cola_servicio` FOREIGN KEY (`servicio_id`) REFERENCES `servicios` (`id`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `historial_turnos`
--
ALTER TABLE `historial_turnos`
  ADD CONSTRAINT `fk_historial_operador` FOREIGN KEY (`operador_id`) REFERENCES `usuarios` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_historial_turno` FOREIGN KEY (`turno_id`) REFERENCES `turnos` (`id`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `modulos_atencion`
--
ALTER TABLE `modulos_atencion`
  ADD CONSTRAINT `fk_modulos_servicio` FOREIGN KEY (`servicio_id`) REFERENCES `servicios` (`id`) ON UPDATE CASCADE;

--
-- Filtros para la tabla `turnos`
--
ALTER TABLE `turnos`
  ADD CONSTRAINT `fk_turnos_categoria` FOREIGN KEY (`categoria_id`) REFERENCES `categorias` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_turnos_modulo` FOREIGN KEY (`modulo_id`) REFERENCES `modulos_atencion` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_turnos_operador` FOREIGN KEY (`operador_id`) REFERENCES `usuarios` (`id`) ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_turnos_servicio` FOREIGN KEY (`servicio_id`) REFERENCES `servicios` (`id`) ON UPDATE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
