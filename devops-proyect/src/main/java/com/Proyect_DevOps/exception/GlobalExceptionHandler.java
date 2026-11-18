package com.Proyect_DevOps.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolationException;

/**
 * Manejador global de excepciones para los controladores REST.
 * Devuelve mensajes controlados al cliente y registra el detalle técnico
 * únicamente en los logs del servidor.
 *
 * @author Equipo DevOps Huellitas
 * @version 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String GENERIC_ERROR = "Error en la solicitud";

    /**
     * Captura excepciones de tipo {@link ResponseStatusException} para respetar el
     * código de estado HTTP y el mensaje funcional definido por el servicio.
     *
     * @param e La excepción ResponseStatusException capturada.
     * @return {@link ResponseEntity} con el código de estado correspondiente.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> manejarResponseStatusException(ResponseStatusException e) {
        String message = e.getReason() != null && !e.getReason().isBlank()
            ? e.getReason()
            : GENERIC_ERROR;
        return ResponseEntity
            .status(e.getStatusCode())
            .body(Map.of("success", false, "message", message));
    }

    /**
     * Captura errores de validación de los DTO y entidades anotadas con Bean Validation.
     *
     * @param e Error de validación.
     * @return Respuesta 400 sin detalles internos.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> manejarValidacion(MethodArgumentNotValidException e) {
        log.warn("Solicitud con datos inválidos: {}", e.getBindingResult().getErrorCount());
        return ResponseEntity.badRequest()
            .body(Map.of("success", false, "message", "Datos de entrada inválidos"));
    }

    /**
     * Captura violaciones de restricciones de validación fuera del cuerpo principal.
     *
     * @param e Violación de restricción.
     * @return Respuesta 400 sin detalles internos.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> manejarConstraintViolation(ConstraintViolationException e) {
        log.warn("Restricción de validación incumplida: {}", e.getConstraintViolations().size());
        return ResponseEntity.badRequest()
            .body(Map.of("success", false, "message", "Datos de entrada inválidos"));
    }

    /**
     * Captura cuerpos JSON mal formados o tipos incompatibles.
     *
     * @param e Error de lectura del cuerpo.
     * @return Respuesta 400 sin detalles internos.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> manejarCuerpoIlegible(HttpMessageNotReadableException e) {
        log.warn("Cuerpo de solicitud ilegible: {}", e.getClass().getSimpleName());
        return ResponseEntity.badRequest()
            .body(Map.of("success", false, "message", "Datos de entrada inválidos"));
    }

    /**
     * Captura excepciones no controladas de tipo {@link RuntimeException}.
     * El mensaje original se registra en el servidor, nunca se envía al cliente,
     * para no filtrar detalles de SQL, criptografía o rutas internas.
     *
     * @param e La excepción en tiempo de ejecución capturada.
     * @return Respuesta 400 con mensaje genérico.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> manejarExcepcion(RuntimeException e) {
        log.error("Error no controlado en la solicitud", e);
        return ResponseEntity
            .badRequest()
            .body(Map.of("success", false, "message", GENERIC_ERROR));
    }
}
