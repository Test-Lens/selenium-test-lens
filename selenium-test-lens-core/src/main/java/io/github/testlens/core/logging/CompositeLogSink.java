package io.github.testlens.core.logging;

import java.util.Arrays;
import java.util.List;

public final class CompositeLogSink implements UiTestLensLogSink {
    private final List<UiTestLensLogSink> sinks;

    public CompositeLogSink(List<UiTestLensLogSink> sinks) {
        this.sinks = sinks == null
                ? List.of()
                : sinks.stream().filter(sink -> sink != null).toList();
    }

    public static CompositeLogSink of(UiTestLensLogSink... sinks) {
        return new CompositeLogSink(sinks == null ? List.of() : Arrays.asList(sinks));
    }

    @Override
    public void accept(UiTestLensLogEntry entry) {
        if (entry == null) {
            return;
        }
        for (UiTestLensLogSink sink : sinks) {
            try {
                sink.accept(entry);
            } catch (RuntimeException ignored) {
                // Logging sinks must not break browser automation tests.
            }
        }
    }
}

