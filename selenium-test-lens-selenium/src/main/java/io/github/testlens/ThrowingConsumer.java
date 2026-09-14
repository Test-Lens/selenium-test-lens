package io.github.testlens;

/** A consumer whose operation may fail with a checked exception. @since 0.3.0 */
@FunctionalInterface
public interface ThrowingConsumer<T> {
    /** Consumes a value or propagates its checked cleanup failure. @since 0.3.0 */
    void accept(T value) throws Exception;
}
