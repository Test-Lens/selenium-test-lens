package io.github.testlens.selenium.auth;

import java.nio.file.Path;

/**
 * Immutable configuration registered under a logical key for managed ensure, refresh and invalidate operations.
 * The key is an identifier, not a credential, but is still omitted from diagnostics and {@code toString()}.
 * A manager rejects reuse of a key with a different canonical path; callback identity is not compared.
 *
 * <pre>{@code
 * AuthStateRequest request = AuthStateRequest.builder()
 *         .key("primary-user")
 *         .path(Path.of("target/auth/primary.json"))
 *         .login(driver -> loginPage.login())
 *         .validate(driver -> accountMenu.isDisplayed()
 *                 ? AuthStateValidation.AUTHENTICATED
 *                 : AuthStateValidation.UNAUTHENTICATED)
 *         .build();
 * }</pre>
 *
 * @since 0.3.0
 */
public final class AuthStateRequest {
    private final String key;
    private final Path path;
    private final AuthStateLogin login;
    private final AuthStateValidator validator;

    private AuthStateRequest(Builder builder) {
        this.key = builder.key.trim();
        this.path = builder.path;
        this.login = builder.login;
        this.validator = builder.validator;
    }

    /**
     * Returns a new validating request builder.
     *
     * @return a new builder
     * @since 0.3.0
     */
    public static Builder builder() { return new Builder(); }
    /**
     * Returns the process-local logical identifier. Treat it as potentially sensitive.
     *
     * @return the non-blank logical identifier
     * @since 0.3.0
     */
    public String key() { return key; }
    /**
     * Returns the persisted auth-state path.
     *
     * @return the configured path
     * @since 0.3.0
     */
    public Path path() { return path; }
    /**
     * Returns the real application-login callback.
     *
     * @return the configured login callback
     * @since 0.3.0
     */
    public AuthStateLogin login() { return login; }
    /**
     * Returns the authoritative application-level validation callback.
     *
     * @return the configured validator
     * @since 0.3.0
     */
    public AuthStateValidator validator() { return validator; }

    /**
     * Returns a representation that omits every configured value.
     *
     * @return a secret-safe representation
     * @since 0.3.0
     */
    @Override public String toString() { return "AuthStateRequest{configured=true}"; }

    /**
     * Builder for a managed auth-state request.
     *
     * @since 0.3.0
     */
    public static final class Builder {
        private String key;
        private Path path;
        private AuthStateLogin login;
        private AuthStateValidator validator;

        private Builder() {}
        /**
         * Sets the non-blank logical lifecycle identifier.
         *
         * @param key the process-local logical identifier
         * @return this builder
         * @since 0.3.0
         */
        public Builder key(String key) { this.key = key; return this; }
        /**
         * Sets the persisted auth-state file.
         *
         * @param path the state file path
         * @return this builder
         * @since 0.3.0
         */
        public Builder path(Path path) { this.path = path; return this; }
        /**
         * Sets the callback that performs the application's real login.
         *
         * @param login the login callback
         * @return this builder
         * @since 0.3.0
         */
        public Builder login(AuthStateLogin login) { this.login = login; return this; }
        /**
         * Sets the authoritative tri-state application validator.
         *
         * @param validator the authentication validator
         * @return this builder
         * @since 0.3.0
         */
        public Builder validate(AuthStateValidator validator) { this.validator = validator; return this; }

        /**
         * Validates all required fields and creates an immutable request.
         *
         * @return the immutable request
         * @throws IllegalArgumentException when any required field is missing or the key is blank
         * @since 0.3.0
         */
        public AuthStateRequest build() {
            if (key == null || key.isBlank()) throw new IllegalArgumentException("key must not be blank");
            if (path == null) throw new IllegalArgumentException("path must not be null");
            if (login == null) throw new IllegalArgumentException("login must not be null");
            if (validator == null) throw new IllegalArgumentException("validate must not be null");
            return new AuthStateRequest(this);
        }
    }
}
