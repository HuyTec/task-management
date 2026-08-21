package com.taskmanagement.exception;

public class InvalidGoogleCredentialException extends RuntimeException {

    public InvalidGoogleCredentialException() {
        super("Invalid Google credential");
    }

    public InvalidGoogleCredentialException(Throwable cause) {
        super("Invalid Google credential", cause);
    }
}
