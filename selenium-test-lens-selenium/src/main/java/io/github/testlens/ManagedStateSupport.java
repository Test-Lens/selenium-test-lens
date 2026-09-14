package io.github.testlens;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

final class ManagedStateSupport {
    private static final AtomicInteger ACTIVE_SCENARIOS = new AtomicInteger();
    private static final AtomicInteger ACTIVE_SUITES = new AtomicInteger();

    private ManagedStateSupport() { }

    static int activeScenarioScopes() { return ACTIVE_SCENARIOS.get(); }
    static int activeSuiteScopes() { return ACTIVE_SUITES.get(); }
    static void scenarioOpened() { ACTIVE_SCENARIOS.incrementAndGet(); }
    static void scenarioClosed() { ACTIVE_SCENARIOS.decrementAndGet(); }
    static void suiteOpened() { ACTIVE_SUITES.incrementAndGet(); }
    static void suiteClosed() { ACTIVE_SUITES.decrementAndGet(); }

    static String requireKey(String key) {
        if (key == null) throw new NullPointerException("State key must not be null");
        if (key.isBlank()) throw new IllegalArgumentException("State key must not be blank");
        return key;
    }

    static final class Store {
        private final String scopeLabel;
        private final Map<String, Object> values = new HashMap<>();
        private boolean active = true;

        Store(String scopeLabel) {
            this.scopeLabel = scopeLabel;
        }

        synchronized <T> void put(String key, T value) {
            ensureActive();
            values.put(requireKey(key), Objects.requireNonNull(value, "State value must not be null"));
        }

        synchronized <T> Optional<T> get(String key, Class<T> type) {
            ensureActive();
            Object value = values.get(requireKey(key));
            if (value == null) return Optional.empty();
            return Optional.of(cast(value, Objects.requireNonNull(type, "type")));
        }

        synchronized <T> T require(String key, Class<T> type) {
            ensureActive();
            Object value = values.get(requireKey(key));
            if (value == null) throw new TestStateException(scopeLabel + " state value is missing");
            return cast(value, Objects.requireNonNull(type, "type"));
        }

        synchronized boolean remove(String key) {
            ensureActive();
            return values.remove(requireKey(key)) != null;
        }

        synchronized boolean contains(String key) {
            ensureActive();
            return values.containsKey(requireKey(key));
        }

        synchronized <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier) {
            ensureActive();
            String checkedKey = requireKey(key);
            Class<T> checkedType = Objects.requireNonNull(type, "type");
            Supplier<? extends T> checkedSupplier = Objects.requireNonNull(supplier, "supplier");
            Object current = values.get(checkedKey);
            if (current != null) return cast(current, checkedType);
            T created = Objects.requireNonNull(checkedSupplier.get(), "State supplier returned null");
            T checked = cast(created, checkedType);
            values.put(checkedKey, checked);
            return checked;
        }

        synchronized int size() {
            ensureActive();
            return values.size();
        }

        synchronized void close() {
            if (!active) return;
            active = false;
            values.clear();
        }

        private void ensureActive() {
            if (!active) throw new TestStateException(scopeLabel + " state scope is closed");
        }

        private <T> T cast(Object value, Class<T> type) {
            if (!type.isInstance(value)) {
                throw new TestStateException(scopeLabel + " state key exists with incompatible type; stored="
                        + value.getClass().getName() + ", requested=" + type.getName());
            }
            return type.cast(value);
        }
    }
}
