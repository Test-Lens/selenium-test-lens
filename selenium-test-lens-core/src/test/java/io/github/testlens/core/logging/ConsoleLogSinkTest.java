package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleLogSinkTest {

    @Test
    void infoGoesToStdout() {
        ConsoleLogSink sink = new ConsoleLogSink();

        CapturedConsole captured = captureConsole();
        try {
            sink.accept(UiTestLensLogEntry.info("info"));

            assertTrue(captured.stdoutText().contains("INFO GENERAL info"));
            assertEquals("", captured.stderrText());
        } finally {
            captured.restore();
        }
    }

    @Test
    void errorGoesToStderr() {
        ConsoleLogSink sink = new ConsoleLogSink();

        CapturedConsole captured = captureConsole();
        try {
            sink.accept(UiTestLensLogEntry.error("error", new RuntimeException("boom")));

            assertEquals("", captured.stdoutText());
            assertTrue(captured.stderrText().contains("ERROR ERROR error"));
        } finally {
            captured.restore();
        }
    }

    private static CapturedConsole captureConsole() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8));
        return new CapturedConsole(originalOut, originalErr, stdout, stderr);
    }

    private record CapturedConsole(
            PrintStream originalOut,
            PrintStream originalErr,
            ByteArrayOutputStream stdout,
            ByteArrayOutputStream stderr
    ) {
        private String stdoutText() {
            return stdout.toString(StandardCharsets.UTF_8);
        }

        private String stderrText() {
            return stderr.toString(StandardCharsets.UTF_8);
        }

        private void restore() {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
