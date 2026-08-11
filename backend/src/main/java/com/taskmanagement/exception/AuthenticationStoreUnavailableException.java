package com.taskmanagement.exception;

public class AuthenticationStoreUnavailableException extends RuntimeException {

    public AuthenticationStoreUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
