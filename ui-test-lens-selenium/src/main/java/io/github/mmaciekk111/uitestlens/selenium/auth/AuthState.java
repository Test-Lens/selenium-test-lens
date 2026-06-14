package io.github.mmaciekk111.uitestlens.selenium.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AuthState {
    private final AuthStateMetadata metadata;
    private final List<AuthCookie> cookies;
    private final List<AuthStorageEntry> localStorage;
    private final List<AuthStorageEntry> sessionStorage;

    public AuthState(AuthStateMetadata metadata,
                     List<AuthCookie> cookies,
                     List<AuthStorageEntry> localStorage,
                     List<AuthStorageEntry> sessionStorage) {
        this.metadata = metadata == null ? AuthStateMetadata.builder().build() : metadata;
        this.cookies = immutableCopy(cookies);
        this.localStorage = immutableCopy(localStorage);
        this.sessionStorage = immutableCopy(sessionStorage);
    }

    public AuthStateMetadata metadata() {
        return metadata;
    }

    public List<AuthCookie> cookies() {
        return cookies;
    }

    public List<AuthStorageEntry> localStorage() {
        return localStorage;
    }

    public List<AuthStorageEntry> sessionStorage() {
        return sessionStorage;
    }

    public String exportJson() {
        return new AuthStateJsonExporter().export(this);
    }

    public Path save(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, exportJson());
            return path;
        } catch (IOException e) {
            throw new AuthStateException("Failed to save auth state: " + e.getMessage(), e);
        }
    }

    public static AuthState load(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        try {
            return new AuthStateJsonParser().parse(Files.readString(path));
        } catch (IOException e) {
            throw new AuthStateException("Failed to load auth state: " + e.getMessage(), e);
        }
    }

    private static <T> List<T> immutableCopy(List<T> input) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }
        List<T> copy = new ArrayList<>();
        for (T item : input) {
            if (item != null) {
                copy.add(item);
            }
        }
        return copy.isEmpty() ? List.of() : Collections.unmodifiableList(copy);
    }
}
