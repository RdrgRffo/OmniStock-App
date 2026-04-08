package com.omnistock.backend.exception;

/**
 * Conflicto al registrar un usuario (username o email ya existente).
 */
public class RegistrationConflictException extends RuntimeException {

    public RegistrationConflictException(String message) {
        super(message);
    }
}
