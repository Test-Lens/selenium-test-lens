package io.github.mmaciekk111.uitestlens.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class JsResources {
    private JsResources() {}

    public static String load(String classpathPath) {
        try (InputStream in = JsResources.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (in == null) throw new IllegalArgumentException("JS resource not found: " + classpathPath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JS resource: " + classpathPath, e);
        }
    }
}