package io.github.testlens.application.tooling.ai.workflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Explicit trusted-host source apply boundary with path and content preconditions. @since 0.5.0 */
public final class ControlledSourceApplier {
    public ApplyResult apply(Path workspace,ApplyRequest request)throws IOException{
        if(workspace==null||request==null)throw new IllegalArgumentException("workspace and request are required");
        if(!request.trustedApply())return new ApplyResult(Status.TRUST_REQUIRED,null,null);
        Path root=workspace.toAbsolutePath().normalize();Files.createDirectories(root);root=root.toRealPath();Path relative=request.relativePath().normalize();if(relative.isAbsolute()||relative.startsWith(".."))throw new IllegalArgumentException("relative path escapes workspace");
        String logical=relative.toString().replace('\\','/');boolean allowed=request.allowedPathPrefixes().stream().anyMatch(prefix->logical.equals(prefix)||logical.startsWith(prefix.endsWith("/")?prefix:prefix+"/"));if(!allowed)return new ApplyResult(Status.PATH_BLOCKED,null,null);
        Path target=root.resolve(relative).normalize();if(!target.startsWith(root)||containsSymbolicLink(root,target))return new ApplyResult(Status.PATH_BLOCKED,null,null);String current=Files.exists(target,LinkOption.NOFOLLOW_LINKS)?Files.readString(target,StandardCharsets.UTF_8):"";String actual=ArtifactEnvelope.digest(current);if(!actual.equals(request.expectedContentFingerprint()))return new ApplyResult(Status.SOURCE_PRECONDITION_FAILED,actual,null);
        Files.createDirectories(target.getParent());if(containsSymbolicLink(root,target))return new ApplyResult(Status.PATH_BLOCKED,null,null);Path temporary=Files.createTempFile(target.getParent(),".test-lens-apply-",".tmp");try{Files.writeString(temporary,request.replacementContent(),StandardCharsets.UTF_8);try{Files.move(temporary,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(java.nio.file.AtomicMoveNotSupportedException ignored){Files.move(temporary,target,StandardCopyOption.REPLACE_EXISTING);}return new ApplyResult(Status.APPLIED,actual,ArtifactEnvelope.digest(request.replacementContent()));}finally{Files.deleteIfExists(temporary);}
    }
    private static boolean containsSymbolicLink(Path root,Path target){Path cursor=root;for(Path segment:root.relativize(target)){cursor=cursor.resolve(segment);if(Files.exists(cursor,LinkOption.NOFOLLOW_LINKS)&&Files.isSymbolicLink(cursor))return true;}return false;}
    public record ApplyRequest(Path relativePath,String expectedContentFingerprint,String replacementContent,List<String>allowedPathPrefixes,boolean trustedApply){public ApplyRequest{if(relativePath==null||expectedContentFingerprint==null||replacementContent==null)throw new IllegalArgumentException("source precondition is required");allowedPathPrefixes=List.copyOf(allowedPathPrefixes==null?List.of():allowedPathPrefixes);}}
    public record ApplyResult(Status status,String previousFingerprint,String appliedFingerprint){}
    public enum Status{APPLIED,TRUST_REQUIRED,PATH_BLOCKED,SOURCE_PRECONDITION_FAILED}
}
