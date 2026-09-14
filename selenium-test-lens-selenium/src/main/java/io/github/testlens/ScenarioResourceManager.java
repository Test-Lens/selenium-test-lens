package io.github.testlens;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Registers resources owned by one test invocation and cleans them once in reverse registration order.
 * Cleanup continues after individual failures. Test Lens preserves an existing test failure as primary; a cleanup
 * failure makes an otherwise passed invocation fail. Names and resource values are not emitted to evidence.
 *
 * <pre>{@code
 * User user = lens.resources().create(
 *         "temporary-user",
 *         api::createUser,
 *         created -> api.deleteUser(created.id()));
 * }</pre>
 *
 * @since 0.3.0
 */
public final class ScenarioResourceManager {
    private final Deque<ResourceEntry<?>> entries = new ArrayDeque<>();
    private State state = State.ACTIVE;

    ScenarioResourceManager() { }

    /**
     * Registers an already-created resource for exactly-once cleanup at invocation finalization.
     * The logical name is validated but is not written to diagnostics or evidence.
     * @param name non-blank logical name, omitted from evidence
     * @param resource non-null resource
     * @param cleanup cleanup callback invoked during finalization
     * @return the registered resource
     * @since 0.3.0
     */
    public synchronized <T> T register(String name, T resource, ThrowingConsumer<? super T> cleanup) {
        ensureActive();
        validateName(name);
        T checkedResource = Objects.requireNonNull(resource, "resource");
        entries.push(new ResourceEntry<>(checkedResource, Objects.requireNonNull(cleanup, "cleanup")));
        return checkedResource;
    }

    /**
     * Creates and registers a resource. A factory failure or null result registers no cleanup callback.
     * @param name non-blank logical name, omitted from evidence
     * @param factory resource factory
     * @param cleanup cleanup callback invoked during finalization
     * @return the created and registered resource
     * @throws Exception when the factory fails
     * @since 0.3.0
     */
    public synchronized <T> T create(String name, ThrowingSupplier<? extends T> factory,
                                     ThrowingConsumer<? super T> cleanup) throws Exception {
        validateName(name);
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(cleanup, "cleanup");
        ensureActive();
        T resource = Objects.requireNonNull(factory.get(), "Resource factory returned null");
        return register(name, resource, cleanup);
    }

    synchronized int size() {
        return entries.size();
    }

    List<Throwable> cleanup() {
        List<ResourceEntry<?>> pending;
        synchronized (this) {
            if (state != State.ACTIVE) return List.of();
            state = State.FINALIZING;
            pending = new ArrayList<>(entries);
            entries.clear();
        }
        List<Throwable> failures = new ArrayList<>();
        for (ResourceEntry<?> entry : pending) {
            try {
                entry.runCleanup();
            } catch (Throwable failure) {
                failures.add(failure);
            }
        }
        synchronized (this) { state = State.CLOSED; }
        return List.copyOf(failures);
    }

    synchronized void closeWithoutCleanup() {
        if (state == State.CLOSED) return;
        entries.clear();
        state = State.CLOSED;
    }

    private void ensureActive() {
        if (state != State.ACTIVE) throw new TestStateException("Scenario resource scope is closing or closed");
    }

    private static void validateName(String name) {
        if (name == null) throw new NullPointerException("Resource name must not be null");
        if (name.isBlank()) throw new IllegalArgumentException("Resource name must not be blank");
    }

    private enum State { ACTIVE, FINALIZING, CLOSED }

    private record ResourceEntry<T>(T value, ThrowingConsumer<? super T> cleanup) {
        void runCleanup() throws Exception { cleanup.accept(value); }
    }
}
