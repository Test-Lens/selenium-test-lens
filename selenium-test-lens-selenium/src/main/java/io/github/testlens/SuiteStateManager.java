package io.github.testlens;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Thread-safe typed state intentionally shared by invocations in one logical runner suite or manual
 * {@link TestRunScope}. Atomic initialization is scoped to this manager; closing its run clears all values.
 * Values are never automatically written to Test Lens evidence.
 *
 * @since 0.3.0
 */
public final class SuiteStateManager {
    private final ManagedStateSupport.Store store = new ManagedStateSupport.Store("Suite");

    SuiteStateManager() { }

    /** Stores a non-null value for this logical suite. @since 0.3.0 */
    public <T> void put(String key, T value) { store.put(key, value); }
    /** Returns the typed suite value, or an empty optional when absent. @since 0.3.0 */
    public <T> Optional<T> get(String key, Class<T> type) { return store.get(key, type); }
    /** Returns the typed suite value or fails when it is absent or incompatible. @since 0.3.0 */
    public <T> T require(String key, Class<T> type) { return store.require(key, type); }
    /** Removes a suite value and reports whether one was present. @since 0.3.0 */
    public boolean remove(String key) { return store.remove(key); }
    /** Reports whether this suite contains the supplied key. @since 0.3.0 */
    public boolean contains(String key) { return store.contains(key); }
    /**
     * Returns the existing typed value or atomically publishes one successful supplier result across callers.
     * A failing supplier or null result is not cached.
     * @since 0.3.0
     */
    public <T> T computeIfAbsent(String key, Class<T> type, Supplier<? extends T> supplier) {
        return store.computeIfAbsent(key, type, supplier);
    }
    /** Returns the number of values held by this logical suite. @since 0.3.0 */
    public int size() { return store.size(); }

    void close() { store.close(); }
}
