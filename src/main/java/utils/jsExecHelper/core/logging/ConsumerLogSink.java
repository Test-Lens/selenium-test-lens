package utils.jsExecHelper.core.logging;

import java.util.function.Consumer;

public final class ConsumerLogSink implements UiTestLensLogSink {
    private final Consumer<UiTestLensLogEntry> consumer;

    public ConsumerLogSink(Consumer<UiTestLensLogEntry> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void accept(UiTestLensLogEntry entry) {
        if (consumer != null && entry != null) {
            consumer.accept(entry);
        }
    }
}
