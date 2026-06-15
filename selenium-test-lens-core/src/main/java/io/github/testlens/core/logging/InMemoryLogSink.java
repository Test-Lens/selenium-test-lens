package io.github.testlens.core.logging;

import io.github.testlens.core.logging.export.HtmlLogExporter;
import io.github.testlens.core.logging.export.JsonLogExporter;
import io.github.testlens.core.logging.export.PlainTextLogExporter;
import io.github.testlens.core.logging.export.UiTestLensLogExporter;
import io.github.testlens.core.trace.export.TraceHtmlExportOptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public Path exportJson(Path outputPath) {
        if (outputPath == null) {
            throw new IllegalArgumentException("outputPath must not be null");
        }
        try {
            Path parent = outputPath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(outputPath, exportAsJson());
            return outputPath;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String exportAsHtml() {
        return export(new HtmlLogExporter());
    }

    public Path exportHtml(Path outputPath) {
        return new HtmlLogExporter().exportTo(entries(), outputPath);
    }

    public Path exportHtml(Path outputPath, TraceHtmlExportOptions options) {
        return new HtmlLogExporter().exportTo(entries(), outputPath, options);
    }

    public Path exportHtmlReport() {
        return new HtmlLogExporter().exportToDefault(entries());
    }

    public Path exportHtmlReport(TraceHtmlExportOptions options) {
        return new HtmlLogExporter().exportToDefault(entries(), options);
    }
}

