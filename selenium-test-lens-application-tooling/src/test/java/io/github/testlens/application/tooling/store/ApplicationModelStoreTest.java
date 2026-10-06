package io.github.testlens.application.tooling.store;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ToolingFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationModelStoreTest {
    @TempDir Path temporary;

    @Test
    void semanticFingerprintIgnoresTimestampAndObservationButChangesWithSemantics() {
        ApplicationModel first = ToolingFixtures.model(Instant.EPOCH, "observation-a", "#login",
                ApplicationModel.SelectorQuality.VERIFIED, true);
        ApplicationModel metadataOnly = ToolingFixtures.model(Instant.parse("2027-01-01T00:00:00Z"), "observation-b", "#login",
                ApplicationModel.SelectorQuality.VERIFIED, true);
        ApplicationModel changed = ToolingFixtures.model(Instant.EPOCH, "observation-a", "[data-testid='login']",
                ApplicationModel.SelectorQuality.VERIFIED, true);

        assertEquals(ApplicationModelFingerprint.semantic(first), ApplicationModelFingerprint.semantic(metadataOnly));
        assertNotEquals(ApplicationModelFingerprint.semantic(first), ApplicationModelFingerprint.semantic(changed));
    }

    @Test
    void writesAtomicallyDeduplicatesHistoryAndBoundsRetention() throws Exception {
        ApplicationModelStore store = new ApplicationModelStore();
        Path modelFile = temporary.resolve("current").resolve("application-model.json");
        store.write(modelFile, ToolingFixtures.model());
        assertEquals(ToolingFixtures.model(), store.read(modelFile));
        try (var files = Files.list(modelFile.getParent())) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }

        Path history = temporary.resolve("history");
        ApplicationModelStore.HistoryResult created = store.recordHistory(history, ToolingFixtures.model(), 1);
        ApplicationModelStore.HistoryResult duplicate = store.recordHistory(history, ToolingFixtures.model(), 1);
        ApplicationModel changed = ToolingFixtures.model(Instant.EPOCH, "other", "#changed",
                ApplicationModel.SelectorQuality.VERIFIED, true);
        ApplicationModelStore.HistoryResult replacement = store.recordHistory(history, changed, 1);

        assertTrue(created.created());
        assertFalse(duplicate.created());
        assertTrue(replacement.created());
        assertEquals(1, replacement.removedEntries());
        try (var files = Files.list(history)) { assertEquals(1, files.filter(path -> path.toString().endsWith(".json")).count()); }
    }

    @Test
    void explicitAllowedRootRejectsTraversalOutsideBoundary() {
        ApplicationModelStore store = new ApplicationModelStore();
        Path allowed = temporary.resolve("allowed");

        assertThrows(ApplicationModelStore.StoreException.class,
                () -> store.write(allowed, Path.of("..").resolve("escaped.json"), ToolingFixtures.model()));
        assertThrows(ApplicationModelStore.StoreException.class,
                () -> store.read(allowed, temporary.resolve("outside.json")));
        assertFalse(Files.exists(temporary.resolve("escaped.json")));
    }

    @Test
    void explicitAllowedRootRejectsSymbolicLinkTraversal() throws Exception {
        Path allowed = Files.createDirectories(temporary.resolve("allowed"));
        Path outside = Files.createDirectories(temporary.resolve("outside"));
        Path link = allowed.resolve("linked");
        try {
            Files.createSymbolicLink(link, outside);
        } catch (java.nio.file.FileSystemException | UnsupportedOperationException failure) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "symbolic links unavailable: " + failure.getMessage());
        }

        ApplicationModelStore store = new ApplicationModelStore();
        assertThrows(ApplicationModelStore.StoreException.class,
                () -> store.write(allowed, link.resolve("model.json"), ToolingFixtures.model()));
        assertFalse(Files.exists(outside.resolve("model.json")));
    }

    @Test
    void historyRetentionOnlyDeletesOwnedEntriesAndPreservesCurrentAndUnrelatedJson() throws Exception {
        ApplicationModelStore store = new ApplicationModelStore();
        Path history = Files.createDirectories(temporary.resolve("history-owned"));
        Path current = history.resolve("application-model.json");
        Path unrelated = history.resolve("consumer-fixture.json");
        Files.writeString(current, "current-model-sentinel");
        Files.writeString(unrelated, "unrelated-sentinel");

        store.recordHistory(history, ToolingFixtures.model(), 1);
        store.recordHistory(history, ToolingFixtures.model(Instant.EPOCH, "next", "#next",
                ApplicationModel.SelectorQuality.VERIFIED, true), 1);

        assertEquals("current-model-sentinel", Files.readString(current));
        assertEquals("unrelated-sentinel", Files.readString(unrelated));
        try (var files = Files.list(history)) {
            List<String> names = files.map(path -> path.getFileName().toString()).sorted().toList();
            assertEquals(3, names.size(), "two caller-owned JSON files plus one retained Test Lens history entry");
            assertEquals(1, names.stream().filter(name -> name.startsWith("model-") && name.endsWith(".json")).count());
        }
    }

    @Test
    void oversizedFileIsRejectedAsStoreExceptionAtPersistenceBoundary() throws Exception {
        Path oversized = temporary.resolve("oversized.json");
        try (var output = Files.newOutputStream(oversized)) {
            output.write(new byte[io.github.testlens.application.tooling.json.StrictJson.MAX_DOCUMENT_BYTES + 1]);
        }

        ApplicationModelStore.StoreException failure = assertThrows(ApplicationModelStore.StoreException.class,
                () -> new ApplicationModelStore().read(temporary, oversized));
        assertTrue(failure.getMessage().toLowerCase(java.util.Locale.ROOT).contains("size"));
    }
}
