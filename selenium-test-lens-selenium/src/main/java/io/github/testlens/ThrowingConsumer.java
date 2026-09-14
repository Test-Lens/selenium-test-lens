package io.github.testlens;

/**
 * A consumer whose operation may fail with a checked exception.
 *
 * @param <T> consumed resource type
 * @since 0.3.0
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {
    /**
     * Consumes a value or propagates its checked cleanup failure.
     *
     * @param value value to consume
     * @throws Exception when the operation fails
     * @since 0.3.0
     */
    void accept(T value) throws Exception;
}
