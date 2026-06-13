package utils.jsExecHelper.core.logging;

import utils.jsExecHelper.core.logging.export.HtmlLogExporter;
import utils.jsExecHelper.core.logging.export.JsonLogExporter;
import utils.jsExecHelper.core.logging.export.PlainTextLogExporter;
import utils.jsExecHelper.core.logging.export.UiTestLensLogExporter;

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

    public String export(UiTestLensLogExporter exporter) {
        if (exporter == null) {
            throw new IllegalArgumentException("exporter must not be null");
        }
        return exporter.export(entries());
    }

    public String exportAsText() {
        return export(new PlainTextLogExporter());
    }

    public String exportAsJson() {
        return export(new JsonLogExporter());
    }

    public String exportAsHtml() {
        return export(new HtmlLogExporter());
    }
}
