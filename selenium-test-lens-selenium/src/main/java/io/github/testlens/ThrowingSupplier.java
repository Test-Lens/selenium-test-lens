package io.github.testlens;

/**
 * A supplier whose operation may fail with a checked exception.
 *
 * @param <T> supplied resource type
 * @since 0.3.0
 */
@FunctionalInterface
public interface ThrowingSupplier<T> {
    /**
     * Returns a value or propagates its checked creation failure.
     *
     * @return supplied value
     * @throws Exception when the operation fails
     * @since 0.3.0
     */
    T get() throws Exception;
}
