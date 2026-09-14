package io.github.testlens;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-safe typed in-memory state owned by one physical test invocation.
 * A retry, repetition, parameter row, or parallel invocation receives a fresh manager. Values are cleared when
 * that invocation is finalized and are never automatically written to Test Lens evidence.
 *
 * <pre>{@code
 * lens.scenarioState().put("orderId", order.id());
 * String orderId = lens.scenarioState().require("orderId", String.class);
 * }</pre>
 *
 * @since 0.3.0
 */
public final class ScenarioStateManager {
    private final ManagedStateSupport.Store store = new ManagedStateSupport.Store("Scenario");

    ScenarioStateManager() { }

    /**
     * Stores a non-null value for this invocation.
     *
     * @param key non-blank key
     * @param value non-null value
     * @since 0.3.0
     */
    public <T> void put(String key, T value) { store.put(key, value); }
    /**
     * Returns the typed value, or an empty optional when the key is absent.
     *
     * @param key key
     * @param type required type
     * @return typed value or empty
     * @since 0.3.0
     */
    public <T> Optional<T> get(String key, Class<T> type) { return store.get(key, type); }
    /**
     * Returns the typed value or fails when it is absent or incompatible.
     *
     * @param key key
     * @param type required type
     * @return typed value
     * @throws TestStateException if missing, incompatible, or closed
     * @since 0.3.0
     */
    public <T> T require(String key, Class<T> type) { return store.require(key, type); }
    /**
     * Removes a value.
     *
     * @param key key
     * @return whether a value was present
     * @since 0.3.0
     */
    public boolean remove(String key) { return store.remove(key); }
    /**
     * Reports whether this invocation contains a key.
     *
     * @param key key
     * @return whether present
     * @since 0.3.0
     */
    public boolean contains(String key) { return store.contains(key); }
    /**
     * Returns the existing typed value or atomically publishes one successful supplier result.
     * A failing supplier or null result is not cached.
     * @param key non-blank key
     * @param type required type
     * @param supplier initializer invoked at most once for a successful publication
     * @return existing or newly published value
     * @throws TestStateException if an existing value is incompatible or the scope is closed
     * @since 0.3.0
     */
    public <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier) {
        return store.computeIfAbsent(key, type, supplier);
    }
    /**
     * Reports the number of values currently held by this invocation.
     *
     * @return number of values
     * @since 0.3.0
     */
    public int size() { return store.size(); }

    void close() { store.close(); }
}
