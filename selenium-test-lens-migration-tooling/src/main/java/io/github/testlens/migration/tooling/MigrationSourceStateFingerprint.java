package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Exact bounded fingerprint of HEAD plus dirty index/worktree state. */
public record MigrationSourceStateFingerprint(int algorithmVersion,String sourceStateDigest,String headTree,
        List<FileState>files,long hashedBytes,Completeness completeness,List<String>issues){
    public static final int ALGORITHM_VERSION=1,MAX_DIRTY_PATHS=10_000;
    public static final long MAX_FILE_BYTES=64L*1024*1024,MAX_TOTAL_BYTES=512L*1024*1024;
    public MigrationSourceStateFingerprint{if(algorithmVersion!=1)throw new IllegalArgumentException("unsupported source-state version");if(sourceStateDigest==null||!sourceStateDigest.matches("migration-source-state-v1:sha256:[0-9a-f]{64}"))throw new IllegalArgumentException("invalid source-state digest");if(headTree!=null&&!headTree.matches("[0-9a-f]{40}|[0-9a-f]{64}"))throw new IllegalArgumentException("headTree");files=files.stream().sorted(java.util.Comparator.comparing(FileState::logicalPath)).toList();if(files.size()>MAX_DIRTY_PATHS)throw new IllegalArgumentException("dirty path bound exceeded");if(hashedBytes<0||hashedBytes>MAX_TOTAL_BYTES)throw new IllegalArgumentException("hashedBytes");Objects.requireNonNull(completeness);issues=issues==null?List.of():issues.stream().distinct().sorted().toList();if(issues.size()>256||issues.stream().anyMatch(x->x==null||x.length()>8_192||x.indexOf('\0')>=0))throw new IllegalArgumentException("issue bound");}
    public enum Completeness{COMPLETE,INCOMPLETE_SOURCE_STATE}
    public enum WorktreeState{REGULAR_FILE,SYMLINK,DELETED,OTHER,NOT_APPLICABLE,UNREADABLE}
    public record IndexState(String mode,int stage,String objectId,String sha256,long size){public IndexState{if(mode==null||!mode.matches("[0-7]{6}"))throw new IllegalArgumentException("index mode");if(stage<0||stage>3)throw new IllegalArgumentException("index stage");if(objectId==null||!objectId.matches("[0-9a-f]{40}|[0-9a-f]{64}"))throw new IllegalArgumentException("index object id");if(sha256==null||!sha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("index sha256");if(size<0||size>MAX_FILE_BYTES)throw new IllegalArgumentException("index size");}}
    public record WorktreeContent(WorktreeState state,String sha256,long size,String linkTargetDigest){public WorktreeContent{Objects.requireNonNull(state);if(sha256!=null&&!sha256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("worktree sha256");if(linkTargetDigest!=null&&!linkTargetDigest.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("link target digest");if(size<0||size>MAX_FILE_BYTES)throw new IllegalArgumentException("worktree size");}}
    public record PathRelation(String originalLogicalPath,MigrationGitPreflight.ChangeKind kind){public PathRelation{originalLogicalPath=MigrationGitPreflight.path(originalLogicalPath);if(kind!=MigrationGitPreflight.ChangeKind.RENAMED&&kind!=MigrationGitPreflight.ChangeKind.COPIED)throw new IllegalArgumentException("rename/copy relation required");}}
    public record FileState(String logicalPath,Set<String>roles,List<PathRelation>relations,IndexState index,WorktreeContent worktree){public FileState{logicalPath=MigrationGitPreflight.path(logicalPath);roles=Set.copyOf(roles);relations=relations==null?List.of():relations.stream().distinct().sorted(java.util.Comparator.comparing(PathRelation::originalLogicalPath).thenComparing(x->x.kind().name())).toList();}public FileState(String logicalPath,Set<String>roles,IndexState index,WorktreeContent worktree){this(logicalPath,roles,List.of(),index,worktree);}}
}
