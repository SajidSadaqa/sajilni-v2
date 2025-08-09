package com.sajilni.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex, Locale locale) {
        String code = ex.getMessage();
        String msg = messages.getMessage(code, null, code, locale);
        return ResponseEntity.badRequest().body(Map.of("error", code, "message", msg));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            IllegalArgumentException.class})
    public ResponseEntity<?> handleValidation(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "validation", "message", ex.getMessage()));
    }
}
