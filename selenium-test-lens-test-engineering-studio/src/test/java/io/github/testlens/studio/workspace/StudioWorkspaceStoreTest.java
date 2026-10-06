package io.github.testlens.studio.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StudioWorkspaceStoreTest {
    @TempDir Path root;

    @Test void persistsDeterministicEnvelopeAndComputesFreshness() throws Exception {
        StudioWorkspaceStore store=new StudioWorkspaceStore(root);
        store.write(StudioWorkspaceStore.ArtifactKind.PROJECT_STATUS,Map.of("status","PASS"),"fingerprint-1");
        assertEquals(StudioWorkspaceStore.Freshness.FRESH,store.freshness(StudioWorkspaceStore.ArtifactKind.PROJECT_STATUS,"fingerprint-1"));
        assertEquals(StudioWorkspaceStore.Freshness.STALE,store.freshness(StudioWorkspaceStore.ArtifactKind.PROJECT_STATUS,"fingerprint-2"));
        assertEquals("PROJECT_STATUS",store.read(StudioWorkspaceStore.ArtifactKind.PROJECT_STATUS).orElseThrow().get("kind"));
        assertTrue(store.workspaceRoot().startsWith(root.toAbsolutePath()));
    }

    @Test void missingArtifactIsNotPretendedFresh() throws Exception {
        assertEquals(StudioWorkspaceStore.Freshness.MISSING,new StudioWorkspaceStore(root).freshness(StudioWorkspaceStore.ArtifactKind.REPAIR_HISTORY,"x"));
    }
}
