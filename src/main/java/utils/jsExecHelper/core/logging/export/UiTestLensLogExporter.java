package utils.jsExecHelper.core.logging.export;

import utils.jsExecHelper.core.logging.UiTestLensLogEntry;

import java.util.List;

public interface UiTestLensLogExporter {
    String export(List<UiTestLensLogEntry> entries);
}
