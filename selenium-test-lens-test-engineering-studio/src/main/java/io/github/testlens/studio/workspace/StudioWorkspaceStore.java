package io.github.testlens.studio.workspace;

import io.github.testlens.application.tooling.json.StrictJson;

import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Deterministic, allowlisted persistence below one explicit project root. */
public final class StudioWorkspaceStore {
    private static final String WORKFLOW_DIRECTORY="ai/sessions";
    private static final java.util.regex.Pattern SAFE_ID=java.util.regex.Pattern.compile("[A-Za-z0-9._-]{1,160}");
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

    public StudioWorkspaceStore(Path projectRoot) { this(projectRoot, Clock.systemUTC(), false); }
    /** Opens an explicitly configured workspace directory rather than appending {@code .test-lens}. */
    public static StudioWorkspaceStore atWorkspace(Path workspaceDirectory) {
        return new StudioWorkspaceStore(workspaceDirectory, Clock.systemUTC(), true);
    }
    StudioWorkspaceStore(Path projectRoot, Clock clock) { this(projectRoot,clock,false); }
    private StudioWorkspaceStore(Path path, Clock clock, boolean exactWorkspace) {
        Objects.requireNonNull(path,"path"); this.clock=Objects.requireNonNull(clock);
        Path normalized=path.toAbsolutePath().normalize();
        this.root=exactWorkspace?normalized:normalized.resolve(".test-lens").normalize();
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

    public synchronized void writeWorkflow(String workflowId,Object snapshot)throws IOException{
        writePath(workflowPath(workflowId),snapshot);
    }

    public synchronized Optional<Map<String,Object>> readWorkflow(String workflowId)throws IOException{
        return readPath(workflowPath(workflowId));
    }

    public synchronized List<String> workflowIds()throws IOException{
        Path directory=root.resolve(WORKFLOW_DIRECTORY).normalize();
        if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))return List.of();
        rejectLinks(directory);try(var files=Files.list(directory)){return files.filter(path->Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))
                .map(path->path.getFileName().toString()).filter(name->name.endsWith(".json"))
                .map(name->name.substring(0,name.length()-5)).filter(name->SAFE_ID.matcher(name).matches()).sorted().limit(500).toList();}
    }

    public Path workspaceRoot(){return root;}
    private Path workflowPath(String workflowId)throws IOException{
        if(workflowId==null||!SAFE_ID.matcher(workflowId).matches())throw new IOException("Invalid workflow id");
        Path path=root.resolve(WORKFLOW_DIRECTORY).resolve(workflowId+".json").normalize();
        if(!path.startsWith(root))throw new IOException("Workflow path escaped root");return path;
    }
    private void writePath(Path target,Object value)throws IOException{
        byte[] payload=StrictJson.write(value);if(payload.length>StrictJson.MAX_DOCUMENT_BYTES)throw new IOException("Studio artifact exceeds JSON limit");
        Files.createDirectories(target.getParent());rejectLinks(target.getParent());Path temporary=Files.createTempFile(target.getParent(),".studio-",".tmp");
        try{Files.write(temporary,payload,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}
        finally{Files.deleteIfExists(temporary);}
    }
    private Optional<Map<String,Object>> readPath(Path target)throws IOException{
        if(!Files.isRegularFile(target,LinkOption.NOFOLLOW_LINKS))return Optional.empty();rejectLinks(target);byte[] bytes=Files.readAllBytes(target);
        if(bytes.length>StrictJson.MAX_DOCUMENT_BYTES)throw new IOException("Studio artifact exceeds JSON limit");return Optional.of(StrictJson.readObject(bytes));
    }
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
