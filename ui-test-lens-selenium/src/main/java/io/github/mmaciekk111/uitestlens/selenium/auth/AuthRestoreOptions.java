package io.github.mmaciekk111.uitestlens.selenium.auth;

public final class AuthRestoreOptions {
    private final boolean navigateToOrigin;
    private final boolean clearExistingCookies;
    private final boolean clearExistingStorage;
    private final boolean restoreCookies;
    private final boolean restoreLocalStorage;
    private final boolean restoreSessionStorage;
    private final boolean validateOrigin;
    private final boolean failIfExpired;

    private AuthRestoreOptions(Builder builder) {
        this.navigateToOrigin = builder.navigateToOrigin;
        this.clearExistingCookies = builder.clearExistingCookies;
        this.clearExistingStorage = builder.clearExistingStorage;
        this.restoreCookies = builder.restoreCookies;
        this.restoreLocalStorage = builder.restoreLocalStorage;
        this.restoreSessionStorage = builder.restoreSessionStorage;
        this.validateOrigin = builder.validateOrigin;
        this.failIfExpired = builder.failIfExpired;
    }

    public static AuthRestoreOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean navigateToOrigin() {
        return navigateToOrigin;
    }

    public boolean clearExistingCookies() {
        return clearExistingCookies;
    }

    public boolean clearExistingStorage() {
        return clearExistingStorage;
    }

    public boolean restoreCookies() {
        return restoreCookies;
    }

    public boolean restoreLocalStorage() {
        return restoreLocalStorage;
    }

    public boolean restoreSessionStorage() {
        return restoreSessionStorage;
    }

    public boolean validateOrigin() {
        return validateOrigin;
    }

    public boolean failIfExpired() {
        return failIfExpired;
    }

    public static final class Builder {
        private boolean navigateToOrigin = true;
        private boolean clearExistingCookies = true;
        private boolean clearExistingStorage = true;
        private boolean restoreCookies = true;
        private boolean restoreLocalStorage = true;
        private boolean restoreSessionStorage = true;
        private boolean validateOrigin = true;
        private boolean failIfExpired = true;

        private Builder() {}

        public Builder navigateToOrigin(boolean navigateToOrigin) {
            this.navigateToOrigin = navigateToOrigin;
            return this;
        }

        public Builder clearExistingCookies(boolean clearExistingCookies) {
            this.clearExistingCookies = clearExistingCookies;
            return this;
        }

        public Builder clearExistingStorage(boolean clearExistingStorage) {
            this.clearExistingStorage = clearExistingStorage;
            return this;
        }

        public Builder restoreCookies(boolean restoreCookies) {
            this.restoreCookies = restoreCookies;
            return this;
        }

        public Builder restoreLocalStorage(boolean restoreLocalStorage) {
            this.restoreLocalStorage = restoreLocalStorage;
            return this;
        }

        public Builder restoreSessionStorage(boolean restoreSessionStorage) {
            this.restoreSessionStorage = restoreSessionStorage;
            return this;
        }

        public Builder validateOrigin(boolean validateOrigin) {
            this.validateOrigin = validateOrigin;
            return this;
        }

        public Builder failIfExpired(boolean failIfExpired) {
            this.failIfExpired = failIfExpired;
            return this;
        }

        public AuthRestoreOptions build() {
            return new AuthRestoreOptions(this);
        }
    }
}
