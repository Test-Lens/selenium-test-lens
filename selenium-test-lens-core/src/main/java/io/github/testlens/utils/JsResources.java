package io.github.testlens.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsResources {
    private JsResources() {}

    public static String load(String classpathPath) {
        try (InputStream in = open(classpathPath)) {
            if (in == null) throw new IllegalArgumentException("JS resource not found: " + classpathPath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JS resource: " + classpathPath, e);
        }
    }

    public static String readFirstExisting(String preferredPath, String legacyPath) {
        String[] paths = { preferredPath, legacyPath };
        for (String path : paths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            try (InputStream in = open(path)) {
                if (in != null) {
                    return new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load JS resource: " + path, e);
            }
        }
        throw new IllegalArgumentException("JS resource not found in preferred or legacy paths: "
                + preferredPath + ", " + legacyPath);
    }

    private static InputStream open(String classpathPath) {
        String normalized = classpathPath != null && classpathPath.startsWith("/")
                ? classpathPath.substring(1)
                : classpathPath;
        return JsResources.class.getClassLoader().getResourceAsStream(normalized);
    }
}
