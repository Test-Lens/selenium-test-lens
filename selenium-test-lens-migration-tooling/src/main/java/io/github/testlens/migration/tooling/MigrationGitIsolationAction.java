package io.github.testlens.migration.tooling;

import java.time.Duration;
import java.util.List;

/** Typed record of an explicitly approved Git isolation mutation. */
public final class MigrationGitIsolationAction {
    private MigrationGitIsolationAction(){}
    public enum Operation{CREATE_BRANCH,CREATE_WORKTREE,CREATE_STASH,RESTORE_STASH}
    public enum Status{APPLIED,SAFE_STOP,PRECONDITION_FAILED,PARTIAL,FAILED}
    public enum Reason{APPROVAL_REQUIRED,REPOSITORY_HELPER_APPROVAL_REQUIRED,BRANCH_ALREADY_EXISTS,INVALID_BRANCH,WORKTREE_DESTINATION_EXISTS,WORKTREE_ALREADY_REGISTERED,ONGOING_GIT_OPERATION,REPOSITORY_BINDING_CHANGED,WORKTREE_BINDING_CHANGED,BASE_REF_CHANGED,STASH_NOT_CREATED,STASH_RESTORE_PRECONDITION_FAILED,STASH_RESTORE_CONFLICT,GIT_COMMAND_FAILED,GIT_TIMEOUT,GIT_OUTPUT_LIMIT,PARTIAL_STATE_OBSERVED}
    public enum StashScope{TRACKED_ONLY,TRACKED_AND_UNTRACKED}
    public record StashRecord(String stashOid,StashScope scope,String repositoryBindingRef,String worktreeBindingRef,String preStashCheckpointRef,String postStashCheckpointRef,String preStashSourceDigest,String postStashSourceDigest){public StashRecord{oid(stashOid);}}
    public record Result(Operation operation,Status status,List<Reason>reasons,String beforeCheckpointRef,String afterCheckpointRef,String branch,String exactBaseCommit,String worktreeBindingRef,StashRecord stashRecord,Duration duration,List<String>issues){public Result{reasons=reasons==null?List.of():reasons.stream().distinct().sorted().toList();issues=issues==null?List.of():issues.stream().distinct().sorted().toList();}}
    static String oid(String value){if(value==null||!value.matches("[0-9a-f]{40}|[0-9a-f]{64}"))throw new IllegalArgumentException("exact object id required");return value;}
}
