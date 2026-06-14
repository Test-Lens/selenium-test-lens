package io.github.mmaciekk111.uitestlens.selenium.auth;

public final class AuthStateException extends RuntimeException {
    public AuthStateException(String message) {
        super(message);
    }

    public AuthStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
