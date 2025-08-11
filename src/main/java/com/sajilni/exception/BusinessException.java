package com.sajilni.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus status;

    public BusinessException(String errorCode) {
        this(errorCode, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, HttpStatus status) {
        super(errorCode);
        this.errorCode = errorCode;
        this.status = status;
    }

    public BusinessException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}