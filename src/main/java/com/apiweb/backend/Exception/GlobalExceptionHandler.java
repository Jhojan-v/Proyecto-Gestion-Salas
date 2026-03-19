package com.apiweb.backend.Exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacion(MethodArgumentNotValidException ex) {
    // 1. Obtener la lista de errores una sola vez
    var erroresDetalle = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> Map.of("campo", err.getField(), "mensaje", err.getDefaultMessage()))
            .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", LocalDateTime.now());
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("message", "Error de validación en los campos enviados");
    body.put("errores", erroresDetalle); // <--- Usamos la variable ya creada

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
}


    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.NOT_FOUND.value());

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFormat(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        // Mensaje amigable para el usuario/frontend
        body.put("message", "Error de formato: El valor enviado no coincide con el tipo de dato esperado (ej. un texto en lugar de un número)");
        
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }
}
