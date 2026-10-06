package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowArtifactStoreTest {
    @TempDir Path temp;

    @Test
    void blocksCanariesAndUnsafeArtifactNames() {
        WorkflowArtifactStore store = new WorkflowArtifactStore();
        RedactionPolicy policy = RedactionPolicy.defaults();

        assertThrows(SecurityException.class, () -> store.store(temp, "run-1",
                new WorkflowArtifactStore.ArtifactDocument("context.json", "{\"value\":\"SESSION-CANARY\"}"),
                policy, List.of("SESSION-CANARY"), new WorkflowArtifactStore.Retention(2)));
        assertThrows(IllegalArgumentException.class, () -> store.store(temp, "run-1",
                new WorkflowArtifactStore.ArtifactDocument("auth-state.json", "{}"), policy, List.of(),
                new WorkflowArtifactStore.Retention(2)));
    }

    @Test
    void retainsOnlyNewestConfiguredRuns() throws Exception {
        WorkflowArtifactStore store = new WorkflowArtifactStore();
        RedactionPolicy policy = RedactionPolicy.defaults();
        var retention = new WorkflowArtifactStore.Retention(2);

        store.store(temp, "run-a", new WorkflowArtifactStore.ArtifactDocument("request.json", "{\"run\":\"a\"}"),
                policy, List.of(), retention);
        Files.setLastModifiedTime(temp.resolve("run-a"), FileTime.fromMillis(1_000));
        store.store(temp, "run-b", new WorkflowArtifactStore.ArtifactDocument("request.json", "{\"run\":\"b\"}"),
                policy, List.of(), retention);
        Files.setLastModifiedTime(temp.resolve("run-b"), FileTime.fromMillis(2_000));
        store.store(temp, "run-c", new WorkflowArtifactStore.ArtifactDocument("request.json", "{\"run\":\"c\"}"),
                policy, List.of(), retention);

        assertFalse(Files.exists(temp.resolve("run-a")));
        assertTrue(Files.exists(temp.resolve("run-b/request.json")));
        assertTrue(Files.exists(temp.resolve("run-c/request.json")));
    }
}
