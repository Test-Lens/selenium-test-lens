package io.github.testlens.core.logging;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UiTestLensLoggerTest {

    @Test
    void noopDoesNotThrow() {
        UiTestLensLogger logger = UiTestLensLogger.noop();

        assertDoesNotThrow(() -> logger.info("info"));
        assertDoesNotThrow(() -> logger.warn("warn"));
        assertDoesNotThrow(() -> logger.error("error"));
        assertDoesNotThrow(() -> logger.emit(UiTestLensLogEntry.info("entry")));
        assertDoesNotThrow(() -> logger.emit(null));
    }

    @Test
    void loggerWithMemorySinkStoresShortcutEntriesInOrder() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder().sink(sink).build();

        logger.info("info");
        logger.warn("warn");
        logger.error("error");

        assertEquals(List.of(
                UiTestLensLogLevel.INFO,
                UiTestLensLogLevel.WARN,
                UiTestLensLogLevel.ERROR
        ), sink.entries().stream().map(UiTestLensLogEntry::level).toList());
        assertEquals(List.of("info", "warn", "error"), sink.entries().stream().map(UiTestLensLogEntry::message).toList());
    }

    @Test
    void loggerEmitsSameEntryToMultipleSinks() {
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(first)
                .sink(second)
                .build();
        UiTestLensLogEntry entry = UiTestLensLogEntry.info("same");

        logger.emit(entry);

        assertNotSame(entry, first.entries().get(0));
        assertSame(first.entries().get(0), second.entries().get(0));
    }

    @Test
    void failingSinkDoesNotBlockNextSink() {
        InMemoryLogSink goodSink = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(entry -> {
                    throw new IllegalStateException("sink failed");
                })
                .sink(goodSink)
                .build();

        assertDoesNotThrow(() -> logger.info("survives"));

        assertEquals(1, goodSink.entries().size());
        assertEquals("survives", goodSink.entries().get(0).message());
    }

    @Test
    void withSinkReturnsNewLoggerAndDoesNotMutateOldOne() {
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger original = UiTestLensLogger.builder().sink(first).build();
        UiTestLensLogger extended = original.withSink(second);

        original.info("original");
        extended.info("extended");

        assertEquals(List.of("original", "extended"), first.entries().stream().map(UiTestLensLogEntry::message).toList());
        assertEquals(List.of("extended"), second.entries().stream().map(UiTestLensLogEntry::message).toList());
    }

    @Test
    void builderIgnoresNullSink() {
        List<UiTestLensLogEntry> received = new ArrayList<>();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .sink(null)
                .sink(received::add)
                .build();

        logger.info("entry");

        assertEquals(1, received.size());
    }

    @Test
    void redactsEveryDiagnosticFieldAndThrowableBeforeFanOutAndThroughWithSink() {
        String secret = "logger-canary-417b";
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        Throwable root = new IllegalArgumentException("root " + secret);
        Throwable failure = new IllegalStateException("failure " + secret, root);
        failure.addSuppressed(new RuntimeException("suppressed " + secret));
        TargetDescriptor target = new TargetDescriptor("#" + secret, "label " + secret,
                "input", "text " + secret, java.util.Map.of("access_token", secret, "other", secret));
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.builder().secret(secret).build()).sink(first).build().withSink(second);

        logger.emit(UiTestLensLogEntry.builder().message("message " + secret).step("step " + secret)
                .action("action " + secret).target(target)
                .metadata(java.util.Map.of("password", secret, "other", secret)).throwable(failure).build());

        assertEquals(1, first.entries().size());
        assertSame(first.entries().get(0), second.entries().get(0));
        UiTestLensLogEntry safe = first.entries().get(0);
        String diagnostic = safe.toString() + safe.target() + safe.metadata() + safe.throwable()
                + safe.throwable().getCause() + java.util.Arrays.toString(safe.throwable().getSuppressed());
        assertFalse(diagnostic.contains(secret));
        assertTrue(diagnostic.contains("[REDACTED]"));
        String exports = new io.github.testlens.core.logging.export.JsonLogExporter().export(List.of(safe))
                + new io.github.testlens.core.logging.export.HtmlLogExporter().export(List.of(safe))
                + new io.github.testlens.core.logging.export.PlainTextLogExporter().export(List.of(safe));
        assertFalse(exports.contains(secret));
        assertTrue(exports.contains("[REDACTED]"));
    }

    @Test
    void structuredJsonIsRedactedBeforeExternalSinkFanOut() {
        String canary = "o'LOGGER_JSON_CANARY";
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder().sink(first).build().withSink(second);

        logger.emit(UiTestLensLogEntry.builder()
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.FAILED)
                .message("{\"password\":\"" + canary + "\"}")
                .build());

        assertSame(first.entries().get(0), second.entries().get(0));
        assertEquals("{\"password\":\"[REDACTED]\"}", first.entries().get(0).message());
        assertEquals(UiTestLensEventType.ACTION, first.entries().get(0).eventType());
        assertEquals(UiTestLensStatus.FAILED, first.entries().get(0).status());
        assertFalse(first.entries().get(0).toString().contains(canary));
    }

    @Test
    void safeThrowablePreservesTypeProvenanceWithoutMutatingOriginalGraph() {
        String rootSecret = "checkout-root-canary";
        String causeSecret = "checkout-cause-canary";
        String suppressedSecret = "checkout-suppressed-canary";
        IllegalArgumentException cause = new IllegalArgumentException(causeSecret);
        CheckoutFailure original = new CheckoutFailure(rootSecret, cause);
        java.util.concurrent.TimeoutException suppressed = new java.util.concurrent.TimeoutException(suppressedSecret);
        original.addSuppressed(suppressed);
        InMemoryLogSink first = new InMemoryLogSink();
        InMemoryLogSink second = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.builder()
                        .secret(rootSecret).secret(causeSecret).secret(suppressedSecret).build())
                .sink(first).sink(second).build();

        logger.emit(UiTestLensLogEntry.builder()
                .metadata("exceptionType", "user.supplied.FakeType")
                .throwable(original)
                .build());

        UiTestLensLogEntry safe = first.entries().get(0);
        assertSame(safe, second.entries().get(0));
        assertNotSame(original, safe.throwable());
        assertNotSame(cause, safe.throwable().getCause());
        assertNotSame(suppressed, safe.throwable().getSuppressed()[0]);
        assertEquals(CheckoutFailure.class.getName(), safe.metadata().get("exceptionType"));
        String safeGraph = safe.throwable() + " " + safe.throwable().getCause() + " "
                + java.util.Arrays.toString(safe.throwable().getSuppressed());
        assertFalse(safeGraph.contains(rootSecret));
        assertFalse(safeGraph.contains(causeSecret));
        assertFalse(safeGraph.contains(suppressedSecret));
        assertTrue(safeGraph.contains(CheckoutFailure.class.getName()));
        assertTrue(safeGraph.contains(IllegalArgumentException.class.getName()));
        assertTrue(safeGraph.contains(java.util.concurrent.TimeoutException.class.getName()));
        assertEquals(rootSecret, original.getMessage());
        assertSame(cause, original.getCause());
        assertSame(suppressed, original.getSuppressed()[0]);
    }

    @Test
    void disabledRedactionStillPreservesSemanticTypeAndMissingThrowableCannotForgeIt() {
        InMemoryLogSink sink = new InMemoryLogSink();
        UiTestLensLogger logger = UiTestLensLogger.builder()
                .redactionPolicy(RedactionPolicy.disabled())
                .sink(sink)
                .build();

        logger.emit(UiTestLensLogEntry.builder()
                .metadata("exceptionType", "user.supplied.FakeType")
                .throwable(new IllegalStateException("visible by explicit opt-out"))
                .build());
        logger.emit(UiTestLensLogEntry.builder()
                .metadata("exceptionType", "user.supplied.FakeType")
                .build());

        assertEquals(IllegalStateException.class.getName(), sink.entries().get(0).metadata().get("exceptionType"));
        assertEquals("visible by explicit opt-out", sink.entries().get(0).throwable().getMessage());
        assertFalse(sink.entries().get(1).metadata().containsKey("exceptionType"));
    }

    private static final class CheckoutFailure extends RuntimeException {
        private CheckoutFailure(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

