package io.github.testlens;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-safe typed in-memory state owned by one physical test invocation.
 * A retry, repetition, parameter row, or parallel invocation receives a fresh manager. Values are cleared when
 * that invocation is finalized and are never automatically written to Test Lens evidence.
 *
 * @since 0.3.0
 */
public final class ScenarioStateManager {
    private final ManagedStateSupport.Store store = new ManagedStateSupport.Store("Scenario");

    ScenarioStateManager() { }

    /** Stores a non-null value for this invocation. @since 0.3.0 */
    public <T> void put(String key, T value) { store.put(key, value); }
    /** Returns the typed value, or an empty optional when the key is absent. @since 0.3.0 */
    public <T> Optional<T> get(String key, Class<T> type) { return store.get(key, type); }
    /** Returns the typed value or fails when it is absent or has an incompatible type. @since 0.3.0 */
    public <T> T require(String key, Class<T> type) { return store.require(key, type); }
    /** Removes a value and reports whether one was present. @since 0.3.0 */
    public boolean remove(String key) { return store.remove(key); }
    /** Reports whether this invocation contains the supplied key. @since 0.3.0 */
    public boolean contains(String key) { return store.contains(key); }
    /**
     * Returns the existing typed value or atomically publishes one successful supplier result.
     * A failing supplier or null result is not cached.
     * @since 0.3.0
     */
    public <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier) {
        return store.computeIfAbsent(key, type, supplier);
    }
    /** Returns the number of values currently held by this invocation. @since 0.3.0 */
    public int size() { return store.size(); }

    void close() { store.close(); }
}
