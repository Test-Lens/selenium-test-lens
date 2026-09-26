package io.github.testlens.selector.engine;

import java.util.List;
import java.util.Objects;

/** Stable structural identity of one component of a locator/candidate. */
public record ComponentIdentity(String componentRole, List<String> componentPath, String componentRef) {
    public ComponentIdentity {
        if (componentRole == null || componentRole.isBlank()) throw new IllegalArgumentException("componentRole is required");
        componentPath = List.copyOf(Objects.requireNonNull(componentPath, "componentPath"));
        if (componentPath.isEmpty() || componentPath.stream().anyMatch(value -> value == null || value.isBlank()))
            throw new IllegalArgumentException("componentPath requires nonblank structural steps");
        String calculated = calculate(componentRole, componentPath);
        if (componentRef == null) componentRef = calculated;
        else if (!componentRef.equals(calculated)) throw new IllegalArgumentException("componentRef does not match structural path");
    }

    public static ComponentIdentity of(String role, String... path) {
        return new ComponentIdentity(role, List.of(path), null);
    }

    private static String calculate(String role, List<String> path) {
        String[] fields = new String[path.size() + 2]; fields[0] = "1"; fields[1] = role;
        for (int i=0;i<path.size();i++) fields[i+2]=path.get(i);
        return "selector-component-v1:sha256:" + CanonicalDigests.digest("selector-component-v1", fields);
    }
}
