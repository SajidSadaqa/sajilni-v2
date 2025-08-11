package com.sajilni.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final MessageSource messages;

    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex, Locale locale) {
        String code = ex.getErrorCode();
        String msg = messages.getMessage(code, null, ex.getMessage(), locale);
        log.warn("Business exception: {} - {}", code, msg);
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", code, "message", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex, Locale locale) {
        String code = ex.getMessage();
        String msg = messages.getMessage(code, null, code, locale);
        log.warn("IllegalStateException: {} - {}", code, msg);
        return ResponseEntity.badRequest().body(Map.of("error", code, "message", msg));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class,
            IllegalArgumentException.class})
    public ResponseEntity<?> handleValidation(Exception ex, Locale locale) {
        String msg = messages.getMessage("validation.error", null, "Validation failed", locale);
        log.warn("Validation exception: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(Map.of("error", "validation", "message", msg));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<?> handleGeneral(Throwable ex, Locale locale) {
        // Handle specific exceptions first
        if (ex instanceof BusinessException businessEx) {
            return handleBusinessException(businessEx, locale);
        }

        if (ex instanceof IllegalStateException illegalStateEx) {
            return handleIllegalState(illegalStateEx, locale);
        }

        if (ex instanceof MethodArgumentNotValidException ||
                ex instanceof ConstraintViolationException ||
                ex instanceof IllegalArgumentException) {
            return handleValidation((Exception) ex, locale);
        }

        // General exception handling
        String msg = messages.getMessage("internal.error", null, "An unexpected error occurred", locale);
        log.error("Unexpected exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "internal_error", "message", msg));
    }
}