package io.github.mmaciekk111.uitestlens.core.logging.export;

import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;

import java.util.List;

public interface UiTestLensLogExporter {
    String export(List<UiTestLensLogEntry> entries);
}
