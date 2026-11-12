package com.banco.app.exception;

import com.banco.app.dto.response.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Manejo centralizado de errores exigido por la documentacion:
// TODAS las respuestas de error del API usan ErrorResponseDTO
// (timestamp, status, error, mensaje, ruta, detallesValidacion).
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 400: fallo de @Valid en el cuerpo de la peticion, con detalle campo por campo
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> validacion(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(HttpStatus.BAD_REQUEST,
                "Falló la validación del cuerpo de la petición", request.getRequestURI(), detalles);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // 400: el cuerpo no es un JSON legible o le falta estructura
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> cuerpoIlegible(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(HttpStatus.BAD_REQUEST,
                "El cuerpo de la petición no es un JSON válido", request.getRequestURI(), null);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    // Errores de negocio lanzados por los servicios con ResponseStatusException
    // (401, 403, 404, 409, ...): se respetan su estado y su mensaje
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDTO> estadoHttp(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus estado = HttpStatus.resolve(ex.getStatusCode().value());
        if (estado == null) {
            estado = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String mensaje = ex.getReason() != null ? ex.getReason() : "Error en la petición";
        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(estado, mensaje, request.getRequestURI(), null);
        return ResponseEntity.status(ex.getStatusCode()).body(cuerpo);
    }

    // 404: ruta inexistente (asi el mensaje de rutas mal escritas tambien es JSON del contrato)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> rutaInexistente(NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(HttpStatus.NOT_FOUND,
                "Ruta no encontrada", request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo);
    }

    // 405: metodo HTTP no soportado en una ruta existente
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> metodoNoSoportado(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(HttpStatus.METHOD_NOT_ALLOWED,
                "Método no permitido para esta ruta", request.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(cuerpo);
    }

    // 500: cualquier error no previsto, con log para poder diagnosticarlo
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> errorInterno(Exception ex, HttpServletRequest request) {
        LOG.error("Error no previsto en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponseDTO cuerpo = ErrorResponseDTO.de(HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor", request.getRequestURI(), null);
        return ResponseEntity.internalServerError().body(cuerpo);
    }
}
