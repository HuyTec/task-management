package com.taskmanagement.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.taskmanagement.dto.Response;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void authenticationStoreUnavailableMapsToServiceUnavailable() {
        AuthenticationStoreUnavailableException exception =
                new AuthenticationStoreUnavailableException("Redis unavailable", null);

        ResponseEntity<Response<Void>> result =
                exceptionHandler.handleAuthenticationStoreUnavailable(exception);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(result.getBody()).isEqualTo(Response.error(
                "Authentication service is temporarily unavailable"
        ));
    }

    @Test
    void immutableSubmissionConflictMapsToHttp409() {
        ResponseEntity<Response<Void>> result = exceptionHandler.handleConflict(
                new ConflictException("Submitted evidence is immutable")
        );

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody()).isEqualTo(Response.error("Submitted evidence is immutable"));
    }
}
