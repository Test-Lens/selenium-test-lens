package io.github.testlens.selenium.auth;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

final class AuthStateRegistrationRegistry {
    record Registration(AuthStateRequest request, Path canonicalPath) {}

    private final ConcurrentHashMap<String, Registration> registrations = new ConcurrentHashMap<>();

    Registration register(AuthStateRequest request, Path canonicalPath) {
        Registration candidate = new Registration(request, canonicalPath);
        Registration registered = registrations.compute(request.key(), (key, existing) -> {
            if (existing != null && !existing.canonicalPath().equals(canonicalPath)) {
                throw new ManagedAuthStateException(ManagedAuthStateFailureReason.REGISTRATION_CONFLICT,
                        "Managed auth state key is already registered for another path");
            }
            return existing == null ? candidate : new Registration(request, canonicalPath);
        });
        return registered;
    }

    Registration require(String key) {
        if (key == null || key.isBlank()) {
            throw unknownKey();
        }
        Registration registration = registrations.get(key.trim());
        if (registration == null) throw unknownKey();
        return registration;
    }

    private static ManagedAuthStateException unknownKey() {
        return new ManagedAuthStateException(ManagedAuthStateFailureReason.UNKNOWN_KEY,
                "Managed auth state key is not registered");
    }
}
