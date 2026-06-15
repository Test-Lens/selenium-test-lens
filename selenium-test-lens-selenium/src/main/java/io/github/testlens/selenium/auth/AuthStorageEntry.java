package io.github.testlens.selenium.auth;

public final class AuthStorageEntry {
    private final String origin;
    private final String key;
    private final String value;
    private final AuthStorageType type;

    public AuthStorageEntry(String origin, String key, String value, AuthStorageType type) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("storage key must not be blank");
        }
        this.origin = origin == null ? "" : origin;
        this.key = key;
        this.value = value == null ? "" : value;
        this.type = type == null ? AuthStorageType.LOCAL_STORAGE : type;
    }

    public String origin() {
        return origin;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    public AuthStorageType type() {
        return type;
    }
}

