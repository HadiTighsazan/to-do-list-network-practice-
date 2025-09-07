package org.example.todo.server.core;

/**
 * Minimal domain exception with a code. Will be mapped to protocol errors later.
 */
public class AppException extends RuntimeException {
    private final String code;

    public AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }

    public static AppException validation(String msg) { return new AppException("VALIDATION_ERROR", msg); }
    public static AppException conflict(String msg) { return new AppException("CONFLICT", msg); }
    public static AppException auth(String msg) { return new AppException("AUTH_INVALID", msg); }
}
