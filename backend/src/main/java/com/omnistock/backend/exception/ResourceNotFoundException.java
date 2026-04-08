package com.omnistock.backend.exception;

/**
 * Excepción lanzada cuando no se encuentra un recurso solicitado.
 * Capturada por {@link GlobalExceptionHandler} y devuelta como HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s no encontrado: %s", resourceName, identifier));
    }
}
