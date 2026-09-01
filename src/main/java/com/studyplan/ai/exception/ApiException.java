package com.studyplan.ai.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Throw this from any service method for an expected error (duplicate
 * email, wrong password, not found, etc). GlobalExceptionHandler catches
 * it and turns it into the right HTTP status + a clean {"message": "..."}
 * JSON body - which is exactly what NewSubject.jsx's error handling
 * expects: err.response?.data?.message
 */
@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
