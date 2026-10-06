package io.github.testlens.studio.workspace;

import io.github.testlens.application.tooling.json.StrictJson;

import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Deterministic, allowlisted persistence below one explicit project root. */
public final class StudioWorkspaceStore {
    public enum ArtifactKind {
        PROJECT_STATUS("project/project-status.json"), APPLICATION_MODEL("project/application-model.json"),
        PAGE_OBJECT_INDEX("project/page-object-index.json"), USAGE_GRAPH("project/usage-graph.json"),
        CORRELATIONS("project/correlations.json"), PROBLEMS("project/problems.json"),
        WORKFLOW_HISTORY("ai/workflow-history.json"), REPAIR_HISTORY("repairs/history.json");
        private final String path; ArtifactKind(String path){this.path=path;} String path(){return path;}
    }
    public enum Freshness { FRESH, STALE, MISSING }
    public record StoredArtifact(ArtifactKind kind, byte[] json, String sourceFingerprint, Instant writtenAt) {
        public StoredArtifact { json=json.clone(); }
        @Override public byte[] json(){return json.clone();}
    }

    private final Path root;
    private final Clock clock;

    public StudioWorkspaceStore(Path projectRoot) { this(projectRoot, Clock.systemUTC()); }
    StudioWorkspaceStore(Path projectRoot, Clock clock) {
        Objects.requireNonNull(projectRoot,"projectRoot"); this.clock=Objects.requireNonNull(clock);
        this.root=projectRoot.toAbsolutePath().normalize().resolve(".test-lens").normalize();
    }

    public synchronized void write(ArtifactKind kind, Object projection, String sourceFingerprint) throws IOException {
        Objects.requireNonNull(kind); Objects.requireNonNull(projection);
        byte[] payload=StrictJson.write(Map.of("schemaVersion",1,"kind",kind.name(),"sourceFingerprint",
                sourceFingerprint==null?"":sourceFingerprint,"writtenAt",clock.instant(),"projection",projection));
        if(payload.length>StrictJson.MAX_DOCUMENT_BYTES)throw new IOException("Studio artifact exceeds JSON limit");
        Path target=target(kind); Files.createDirectories(target.getParent()); rejectLinks(target.getParent());
        Path temporary=Files.createTempFile(target.getParent(),".studio-",".tmp");
        try { Files.write(temporary,payload,StandardOpenOption.TRUNCATE_EXISTING); try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);} }
        finally { Files.deleteIfExists(temporary); }
    }

    public synchronized Optional<Map<String,Object>> read(ArtifactKind kind) throws IOException {
        Path target=target(kind); if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))return Optional.empty();
        rejectLinks(target); byte[] bytes=Files.readAllBytes(target);
        if(bytes.length>StrictJson.MAX_DOCUMENT_BYTES)throw new IOException("Studio artifact exceeds JSON limit");
        return Optional.of(StrictJson.readObject(bytes));
    }

    public synchronized Freshness freshness(ArtifactKind kind, String currentFingerprint) throws IOException {
        Optional<Map<String,Object>> stored=read(kind); if(stored.isEmpty())return Freshness.MISSING;
        Object previous=stored.get().get("sourceFingerprint");
        return currentFingerprint!=null&&currentFingerprint.equals(previous)?Freshness.FRESH:Freshness.STALE;
    }

    public Path workspaceRoot(){return root;}
    private Path target(ArtifactKind kind) throws IOException {
        Path target=root.resolve(kind.path()).normalize();
        if(!target.startsWith(root))throw new IOException("Workspace path escaped root");
        return target;
    }
    private void rejectLinks(Path target) throws IOException {
        Path cursor=root.getParent();
        if(cursor==null)return;
        Path normalized=target.toAbsolutePath().normalize();
        for(Path part:cursor.relativize(normalized)){cursor=cursor.resolve(part);if(Files.exists(cursor,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(cursor))throw new IOException("Symbolic links are not allowed in Studio workspace");}
    }
}
