package io.github.testlens.application.tooling.ai.workflow;

import io.github.testlens.application.tooling.ai.security.AgentArtifactSecurityGate;
import io.github.testlens.core.redaction.RedactionPolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Bounded host-side store for already-redacted workflow artifacts. It never accepts auth/browser state artifacts. @since 0.5.0 */
public final class WorkflowArtifactStore {
    private static final Set<String>ALLOWED=Set.of("request.json","context.json","test-plan.json","implementation-proposal.json","execution-result.json","failure-classification.json","repair-proposal.json","review-result.json","metrics.json");
    private final AgentArtifactSecurityGate gate=new AgentArtifactSecurityGate();
    public StoredArtifact store(Path root,String runId,ArtifactDocument artifact,RedactionPolicy redaction,List<String>canaries,Retention retention)throws IOException{
        if(root==null||runId==null||!runId.matches("[A-Za-z0-9._-]{1,96}"))throw new IllegalArgumentException("safe root and runId are required");
        if(artifact==null||!ALLOWED.contains(artifact.name()))throw new IllegalArgumentException("unsupported workflow artifact name");
        var security=gate.validate(artifact.content(),List.of(artifact.name()),redaction,canaries);if(security.status()!=AgentArtifactSecurityGate.Status.PASS)throw new SecurityException("Workflow artifact blocked: "+security.findings().stream().map(AgentArtifactSecurityGate.Finding::code).toList());
        Path normalizedRoot=root.toAbsolutePath().normalize();Files.createDirectories(normalizedRoot);normalizedRoot=normalizedRoot.toRealPath();Path run=normalizedRoot.resolve(runId).normalize();if(!run.startsWith(normalizedRoot))throw new IllegalArgumentException("run path escapes artifact root");if(Files.exists(run)&&(Files.isSymbolicLink(run)||!run.toRealPath().startsWith(normalizedRoot)))throw new SecurityException("run directory must not be a link");Files.createDirectories(run);if(!run.toRealPath().startsWith(normalizedRoot))throw new SecurityException("run directory escapes artifact root");
        Path target=run.resolve(artifact.name());if(Files.exists(target,java.nio.file.LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(target))throw new SecurityException("artifact destination must not be a link");Path temporary=Files.createTempFile(run,".test-lens-artifact-",".tmp");try{Files.writeString(temporary,artifact.content(),StandardCharsets.UTF_8);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temporary);}
        enforceRetention(normalizedRoot,retention.maxRuns());return new StoredArtifact(normalizedRoot.relativize(target).toString().replace('\\','/'),ArtifactEnvelope.digest(artifact.content()),Files.size(target));
    }
    private static void enforceRetention(Path root,int maxRuns)throws IOException{try(var stream=Files.list(root)){var runs=stream.filter(path->Files.isDirectory(path)&&!Files.isSymbolicLink(path)).sorted(Comparator.comparingLong(WorkflowArtifactStore::lastModified).thenComparing(Path::toString)).toList();for(int i=0;i<Math.max(0,runs.size()-maxRuns);i++)deleteTree(runs.get(i));}}
    private static long lastModified(Path value){try{return Files.getLastModifiedTime(value).toMillis();}catch(IOException ignored){return Long.MIN_VALUE;}}
    private static void deleteTree(Path root)throws IOException{try(var stream=Files.walk(root)){for(Path path:stream.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(path);}}
    public record ArtifactDocument(String name,String content){public ArtifactDocument{if(name==null||content==null)throw new IllegalArgumentException("name and content are required");}}
    public record Retention(int maxRuns){public Retention{if(maxRuns<1||maxRuns>1_000)throw new IllegalArgumentException("maxRuns must be in [1,1000]");}}
    public record StoredArtifact(String logicalPath,String sha256,long bytes){}
}
