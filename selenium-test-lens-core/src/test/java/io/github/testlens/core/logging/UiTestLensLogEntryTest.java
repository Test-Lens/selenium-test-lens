package io.github.testlens.core.logging;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UiTestLensLogEntryTest {

    @Test
    void infoCreatesGeneralInfoEntry() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.info("hello");

        assertEquals(UiTestLensLogLevel.INFO, entry.level());
        assertEquals(UiTestLensEventType.GENERAL, entry.eventType());
        assertEquals(UiTestLensStatus.INFO, entry.status());
        assertEquals("hello", entry.message());
        assertNotNull(entry.timestamp());
        assertEquals(TargetDescriptor.none(), entry.target());
        assertTrue(entry.metadata().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> entry.metadata().put("x", "y"));
    }

    @Test
    void warnCreatesWarnEntry() {
        UiTestLensLogEntry entry = UiTestLensLogEntry.warn("careful");

        assertEquals(UiTestLensLogLevel.WARN, entry.level());
        assertEquals(UiTestLensStatus.WARN, entry.status());
        assertEquals("careful", entry.message());
    }

    @Test
    void errorCreatesFailedErrorEntryWithThrowable() {
        RuntimeException throwable = new RuntimeException("boom");

        UiTestLensLogEntry entry = UiTestLensLogEntry.error("failed", throwable);

        assertEquals(UiTestLensLogLevel.ERROR, entry.level());
        assertEquals(UiTestLensEventType.ERROR, entry.eventType());
        assertEquals(UiTestLensStatus.FAILED, entry.status());
        assertSame(throwable, entry.throwable());
    }

    @Test
    void builderCopiesMetadataDefensively() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("a", "1");

        UiTestLensLogEntry entry = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.DEBUG)
                .eventType(UiTestLensEventType.ACTION)
                .status(UiTestLensStatus.STARTED)
                .step("step")
                .action("click")
                .target(TargetDescriptor.label("Save"))
                .metadata(metadata)
                .metadata("b", "2")
                .build();

        metadata.put("a", "changed");

        assertEquals(UiTestLensLogLevel.DEBUG, entry.level());
        assertEquals(UiTestLensEventType.ACTION, entry.eventType());
        assertEquals(UiTestLensStatus.STARTED, entry.status());
        assertEquals("step", entry.step());
        assertEquals("click", entry.action());
        assertEquals("Save", entry.target().label());
        assertEquals(Map.of("a", "1", "b", "2"), entry.metadata());
        assertThrows(UnsupportedOperationException.class, () -> entry.metadata().put("c", "3"));
    }

    @Test
    void toBuilderCopiesFieldsAndDoesNotMutateOriginal() {
        UiTestLensLogEntry original = UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.STARTED)
                .message("before")
                .metadata("key", "value")
                .build();

        UiTestLensLogEntry changed = original.toBuilder()
                .status(UiTestLensStatus.PASSED)
                .message("after")
                .build();

        assertEquals(UiTestLensStatus.STARTED, original.status());
        assertEquals("before", original.message());
        assertEquals(UiTestLensStatus.PASSED, changed.status());
        assertEquals("after", changed.message());
        assertEquals(original.level(), changed.level());
        assertEquals(original.eventType(), changed.eventType());
        assertEquals(original.metadata(), changed.metadata());
    }
}
