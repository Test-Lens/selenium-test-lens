package io.github.testlens.core.logging.export;

import io.github.testlens.core.logging.UiTestLensLogEntry;

import java.util.List;

public interface UiTestLensLogExporter {
    String export(List<UiTestLensLogEntry> entries);
}
