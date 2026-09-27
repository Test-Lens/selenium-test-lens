package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Canonical path-sanitized result of a read-only Git preflight. */
public record MigrationGitPreflight(RepositoryStatus repositoryStatus,String repositoryBindingRef,
        String worktreeBindingRef,String gitVersion,String objectFormat,Head head,String upstreamRef,
        List<StatusEntry>staged,List<StatusEntry>unstaged,List<StatusEntry>untracked,List<StatusEntry>conflicted,
        List<RenameCopy>renamesAndCopies,List<GitOperation>ongoingOperations,List<Worktree>worktrees,
        SparseCheckout sparseCheckout,SubmoduleSummary submodules,Completeness completeness,List<Issue>issues){
    public static final int MAX_PATHS=10_000,MAX_WORKTREES=1_024,MAX_ISSUES=256;
    public MigrationGitPreflight{Objects.requireNonNull(repositoryStatus);if(repositoryStatus==RepositoryStatus.DETECTED){binding(repositoryBindingRef,"migration-repository-binding-v1");binding(worktreeBindingRef,"migration-worktree-binding-v1");if(gitVersion==null||gitVersion.isBlank())throw new IllegalArgumentException("gitVersion");if(!"sha1".equals(objectFormat)&&!"sha256".equals(objectFormat))throw new IllegalArgumentException("objectFormat");}head=Objects.requireNonNull(head);staged=copy(staged,MAX_PATHS);unstaged=copy(unstaged,MAX_PATHS);untracked=copy(untracked,MAX_PATHS);conflicted=copy(conflicted,MAX_PATHS);renamesAndCopies=copy(renamesAndCopies,MAX_PATHS);ongoingOperations=copy(ongoingOperations,8);worktrees=copy(worktrees,MAX_WORKTREES);Objects.requireNonNull(sparseCheckout);Objects.requireNonNull(submodules);Objects.requireNonNull(completeness);issues=copy(issues,MAX_ISSUES);}
    public enum RepositoryStatus{DETECTED,NOT_A_REPOSITORY,NOT_TRUSTED,UNSUPPORTED_GIT_VERSION,ERROR}
    public enum HeadState{NAMED_BRANCH,DETACHED,UNBORN}
    public enum ChangeKind{ADDED,MODIFIED,DELETED,RENAMED,COPIED,TYPE_CHANGED,UNMERGED,UNTRACKED,UNKNOWN}
    public enum GitOperation{MERGE,REBASE_MERGE,REBASE_APPLY,CHERRY_PICK,REVERT,BISECT}
    public enum SparseCheckout{ENABLED,DISABLED,UNKNOWN}
    public enum Completeness{COMPLETE,PARTIAL,INCOMPLETE}
    public record Head(HeadState state,String commit,String tree,String branch){public Head{Objects.requireNonNull(state);}}
    public record StatusEntry(String logicalPath,ChangeKind kind,String indexCode,String worktreeCode,String indexMode,String indexObjectId){public StatusEntry{logicalPath=path(logicalPath);Objects.requireNonNull(kind);}}
    public record RenameCopy(String logicalPath,String originalLogicalPath,ChangeKind kind){public RenameCopy{logicalPath=path(logicalPath);originalLogicalPath=path(originalLogicalPath);if(kind!=ChangeKind.RENAMED&&kind!=ChangeKind.COPIED)throw new IllegalArgumentException("rename/copy kind required");}}
    public record Worktree(String bindingRef,String head,String branch,boolean detached,boolean locked,boolean prunable,boolean current){ }
    public record SubmoduleSummary(boolean present,Completeness completeness,int gitlinks,List<String>limitations){public SubmoduleSummary{Objects.requireNonNull(completeness);limitations=copy(limitations,32);}}
    public record Issue(String code,String detail){public Issue{code=safe(code,128);detail=safe(detail,512);}}
    static String path(String value){value=safe(value,4096);String securityPath=value.replace('\\','/');if(value.isBlank()||securityPath.startsWith("/")||securityPath.matches("^[A-Za-z]:.*")||securityPath.equals(".git")||securityPath.startsWith(".git/")||securityPath.split("/",-1).length>512)throw new IllegalArgumentException("unsafe Git logical path");for(String s:securityPath.split("/"))if(s.equals(".."))throw new IllegalArgumentException("Git path traversal");return value;}
    private static String safe(String v,int n){v=v==null?"":v;if(v.codePointCount(0,v.length())>n||v.indexOf('\0')>=0)throw new IllegalArgumentException("unsafe value");return v;}
    private static void binding(String value,String domain){if(value==null||!value.matches(java.util.regex.Pattern.quote(domain)+":sha256:[0-9a-f]{64}"))throw new IllegalArgumentException(domain);}
    private static <T>List<T>copy(List<T>v,int n){v=v==null?List.of():List.copyOf(v);if(v.size()>n)throw new IllegalArgumentException("collection bound exceeded");return v;}
}
