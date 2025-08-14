package com.sajilni.exception;

import com.sajilni.domain.response.ErrorResponse;
import com.sajilni.domain.response.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String localizedMessage = messageSource.getMessage(
                ex.getErrorCode(), null, ex.getMessage(), locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(ex.getStatus().value())
                .error(ex.getStatus().getReasonPhrase())
                .code(ex.getErrorCode())
                .message(localizedMessage)
                .path(request.getRequestURI())
                .build();

        log.warn("Business exception [{}]: {} at {}",
                ex.getErrorCode(), localizedMessage, request.getRequestURI());

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();

        Map<String, List<String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.groupingBy(
                        FieldError::getField,
                        Collectors.mapping(
                                error -> messageSource.getMessage(error, locale),
                                Collectors.toList()
                        )
                ));

        List<String> globalErrors = ex.getBindingResult()
                .getGlobalErrors()
                .stream()
                .map(error -> messageSource.getMessage(error, locale))
                .collect(Collectors.toList());

        String mainMessage = messageSource.getMessage(
                "validation.error", null, "Validation failed", locale);

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(mainMessage)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .globalErrors(globalErrors)
                .build();

        log.warn("Validation failed at {}: {} field errors, {} global errors",
                request.getRequestURI(), fieldErrors.size(), globalErrors.size());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();

        Map<String, List<String>> fieldErrors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.groupingBy(
                        violation -> getFieldName(violation),
                        Collectors.mapping(
                                violation -> messageSource.getMessage(
                                        violation.getMessage(), null, violation.getMessage(), locale),
                                Collectors.toList()
                        )
                ));

        String mainMessage = messageSource.getMessage(
                "validation.error", null, "Validation failed", locale);

        ValidationErrorResponse errorResponse = ValidationErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(mainMessage)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .globalErrors(Collections.emptyList())
                .build();

        log.warn("Constraint violation at {}: {} violations",
                request.getRequestURI(), fieldErrors.size());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "auth.bad", null, "Invalid credentials", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .code("auth.bad")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler({DisabledException.class})
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "auth.unverified", null, "Account not verified", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code("auth.unverified")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Disabled account login attempt at {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler({LockedException.class})
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "account.locked", null, "Account is locked", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.LOCKED.value())
                .error("Locked")
                .code("account.locked")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Locked account login attempt at {}", request.getRequestURI());

        return ResponseEntity.status(HttpStatus.LOCKED).body(errorResponse);
    }

    @ExceptionHandler({AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "access.denied", null, "Access denied", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .code("access.denied")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String code = ex.getMessage();
        String message = messageSource.getMessage(code, null, code, locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code(code)
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Illegal state at {}: {}", request.getRequestURI(), message);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "validation.error", null, ex.getMessage(), locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .code("validation.error")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {

        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(
                "internal.error", null, "An unexpected error occurred", locale);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .code("internal.error")
                .message(message)
                .path(request.getRequestURI())
                .build();

        log.error("Unexpected error at {}: ", request.getRequestURI(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String getFieldName(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();
        return propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
    }
}
