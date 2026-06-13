package utils.jsExecHelper.core.logging;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class InMemoryLogSink implements UiTestLensLogSink {
    private final CopyOnWriteArrayList<UiTestLensLogEntry> entries = new CopyOnWriteArrayList<>();

    @Override
    public void accept(UiTestLensLogEntry entry) {
        if (entry != null) {
            entries.add(entry);
        }
    }

    public List<UiTestLensLogEntry> entries() {
        return List.copyOf(entries);
    }

    public void clear() {
        entries.clear();
    }
}
