package com.Proyect_DevOps.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    @Test
    void hidesInternalExceptionDetails() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<java.util.Map<String, Object>> response = handler.manejarExcepcion(
            new RuntimeException("SQL error: password=super-secret at /internal/path")
        );

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Error en la solicitud", response.getBody().get("message"));
        assertFalse(response.getBody().toString().contains("super-secret"));
        assertFalse(response.getBody().toString().contains("/internal/path"));
    }
}
