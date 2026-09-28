package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Read-only manual-commit assessment. It never stages or commits files. */
public record MigrationCommitReadiness(State state,List<String>reasons,List<String>guidance,String suggestedMessage){
    public MigrationCommitReadiness{Objects.requireNonNull(state);reasons=strings(reasons);guidance=strings(guidance);if(suggestedMessage!=null&&(suggestedMessage.length()>256||suggestedMessage.indexOf('\0')>=0))throw new IllegalArgumentException("message");}
    public enum State{COMMIT_READY_MANUAL,MANUAL_COMMIT_REQUIRES_REVIEW,COMMIT_BLOCKED}
    private static List<String>strings(List<String>v){return v==null?List.of():v.stream().filter(Objects::nonNull).distinct().sorted().toList();}
}
