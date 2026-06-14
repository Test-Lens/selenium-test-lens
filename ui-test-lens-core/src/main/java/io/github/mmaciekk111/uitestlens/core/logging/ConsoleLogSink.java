package io.github.mmaciekk111.uitestlens.core.logging;

import java.io.PrintStream;

public final class ConsoleLogSink implements UiTestLensLogSink {
    @Override
    public void accept(UiTestLensLogEntry entry) {
        if (entry == null) {
            return;
        }
        PrintStream stream = entry.level() == UiTestLensLogLevel.ERROR ? System.err : System.out;
        stream.println(format(entry));
    }

    private String format(UiTestLensLogEntry entry) {
        return entry.timestamp()
                + " "
                + entry.level()
                + " "
                + entry.eventType()
                + " "
                + entry.message();
    }
}
