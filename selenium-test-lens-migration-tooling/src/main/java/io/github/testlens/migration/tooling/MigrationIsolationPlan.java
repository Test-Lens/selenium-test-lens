package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Pure plan. No method in this type performs a Git mutation. */
public record MigrationIsolationPlan(Choice choice,boolean recommended,String branchCandidate,
        String worktreeDestinationBindingRef,String baseRef,List<String>reasonCodes){
    public MigrationIsolationPlan{Objects.requireNonNull(choice);reasonCodes=reasonCodes==null?List.of():reasonCodes.stream().distinct().sorted().limit(32).toList();}
    public enum Choice{USE_CURRENT_WORKTREE,CREATE_DEDICATED_BRANCH,CREATE_DEDICATED_WORKTREE,STASH_EXPLICITLY,MANUAL_RESOLUTION_REQUIRED}
    public static MigrationIsolationPlan recommend(MigrationGitPreflight p){
        if(p.repositoryStatus()!=MigrationGitPreflight.RepositoryStatus.DETECTED||p.completeness()!=MigrationGitPreflight.Completeness.COMPLETE)return new MigrationIsolationPlan(Choice.MANUAL_RESOLUTION_REQUIRED,true,null,null,p.head().commit(),List.of("PREFLIGHT_INCOMPLETE"));
        if(!p.ongoingOperations().isEmpty()||!p.conflicted().isEmpty())return new MigrationIsolationPlan(Choice.MANUAL_RESOLUTION_REQUIRED,true,null,null,p.head().commit(),List.of("GIT_OPERATION_OR_CONFLICT_IN_PROGRESS"));
        boolean dirty=!p.staged().isEmpty()||!p.unstaged().isEmpty()||!p.untracked().isEmpty();return dirty?new MigrationIsolationPlan(Choice.CREATE_DEDICATED_WORKTREE,true,null,null,p.head().commit(),List.of("CURRENT_WORKTREE_DIRTY")):new MigrationIsolationPlan(Choice.USE_CURRENT_WORKTREE,true,null,null,p.head().commit(),List.of("CURRENT_WORKTREE_CLEAN"));
    }
}
