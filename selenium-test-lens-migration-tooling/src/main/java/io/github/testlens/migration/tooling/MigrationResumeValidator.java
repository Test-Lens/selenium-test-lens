package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Compares a durable checkpoint with a freshly captured read-only state. */
public final class MigrationResumeValidator {
    public enum Reason{HEAD_CHANGED,BRANCH_CHANGED,WORKTREE_CHANGED,INDEX_CHANGED,WORKING_FILE_CHANGED,UNTRACKED_CHANGED,GIT_OPERATION_CHANGED,INPUT_REPORT_CHANGED,SOURCE_PRECONDITION_CHANGED,MISSING_ARTIFACT,REPOSITORY_BINDING_CHANGED,CHECKPOINT_INCOMPLETE}
    public enum Classification{VALID,VALID_WITH_WARNINGS,REQUIRES_REFRESH,BLOCKED}
    public record Result(Classification classification,List<Reason>reasons,List<String>changedPaths){public Result{reasons=List.copyOf(reasons);changedPaths=changedPaths.stream().distinct().sorted().limit(10_000).toList();}}
    public Result validate(MigrationCheckpoint old,MigrationGitPreflight fresh,MigrationSourceStateFingerprint source,List<MigrationArtifactRef>artifacts){EnumSet<Reason>reasons=EnumSet.noneOf(Reason.class);List<String>paths=new ArrayList<>();if(!Objects.equals(old.repositoryBindingRef(),fresh.repositoryBindingRef()))reasons.add(Reason.REPOSITORY_BINDING_CHANGED);if(!Objects.equals(old.worktreeBindingRef(),fresh.worktreeBindingRef()))reasons.add(Reason.WORKTREE_CHANGED);if(old.head().state()!=fresh.head().state()||!Objects.equals(old.head().commit(),fresh.head().commit())||!Objects.equals(old.head().tree(),fresh.head().tree()))reasons.add(Reason.HEAD_CHANGED);if(!Objects.equals(old.head().branch(),fresh.head().branch()))reasons.add(Reason.BRANCH_CHANGED);if(!old.gitOperations().equals(fresh.ongoingOperations()))reasons.add(Reason.GIT_OPERATION_CHANGED);if(old.completeness()!=MigrationCheckpoint.Completeness.COMPLETE||fresh.completeness()!=MigrationGitPreflight.Completeness.COMPLETE||source==null||source.completeness()!=MigrationSourceStateFingerprint.Completeness.COMPLETE)reasons.add(Reason.CHECKPOINT_INCOMPLETE);
        Map<String,MigrationSourceStateFingerprint.FileState>a=map(old.sourceState().files()),b=source==null?Map.of():map(source.files());for(String path:union(a,b)){var x=a.get(path);var y=b.get(path);if(Objects.equals(x,y))continue;paths.add(path);boolean xi=hasRole(x,"STAGED")||hasRole(x,"CONFLICTED"),yi=hasRole(y,"STAGED")||hasRole(y,"CONFLICTED");if(xi||yi||!Objects.equals(x==null?null:x.index(),y==null?null:y.index()))reasons.add(Reason.INDEX_CHANGED);boolean xu=hasRole(x,"UNTRACKED"),yu=hasRole(y,"UNTRACKED");if(xu||yu)reasons.add(Reason.UNTRACKED_CHANGED);boolean xw=hasRole(x,"UNSTAGED"),yw=hasRole(y,"UNSTAGED");if(xw||yw||(!xu&&!yu&&!xi&&!yi))reasons.add(Reason.WORKING_FILE_CHANGED);}if(source!=null&&!old.sourceState().sourceStateDigest().equals(source.sourceStateDigest())&&paths.isEmpty())reasons.add(Reason.SOURCE_PRECONDITION_CHANGED);
        Map<String,MigrationArtifactRef>current=new HashMap<>();if(artifacts!=null)artifacts.forEach(x->current.put(x.logicalRef(),x));for(MigrationArtifactRef expected:old.artifacts()){MigrationArtifactRef actual=current.get(expected.logicalRef());if(actual==null)reasons.add(Reason.MISSING_ARTIFACT);else if(!expected.equals(actual))reasons.add(Reason.INPUT_REPORT_CHANGED);}
        Classification classification;if(reasons.isEmpty())classification=Classification.VALID;else if(reasons.contains(Reason.REPOSITORY_BINDING_CHANGED)||reasons.contains(Reason.WORKTREE_CHANGED)||reasons.contains(Reason.GIT_OPERATION_CHANGED)||reasons.contains(Reason.MISSING_ARTIFACT)||reasons.contains(Reason.CHECKPOINT_INCOMPLETE))classification=Classification.BLOCKED;else classification=Classification.REQUIRES_REFRESH;return new Result(classification,reasons.stream().sorted(Comparator.comparing(Enum::name)).toList(),paths);}
    private static boolean hasRole(MigrationSourceStateFingerprint.FileState state,String role){return state!=null&&state.roles().contains(role);}
    private static Map<String,MigrationSourceStateFingerprint.FileState>map(List<MigrationSourceStateFingerprint.FileState>v){Map<String,MigrationSourceStateFingerprint.FileState>m=new HashMap<>();v.forEach(x->m.put(x.logicalPath(),x));return m;}
    private static java.util.Set<String>union(Map<String,?>a,Map<String,?>b){java.util.Set<String>s=new java.util.TreeSet<>(a.keySet());s.addAll(b.keySet());return s;}
}
