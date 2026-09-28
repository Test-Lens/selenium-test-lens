package io.github.testlens.migration.tooling;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Human-usable, read-only projection of B1 recovery inspection. */
public record MigrationRecoverySummary(Situation situation,List<FileRecovery>files,
        List<MigrationRecoveryActionPlan.Action>availableActions,List<String>issues){
    public MigrationRecoverySummary{Objects.requireNonNull(situation);files=files==null?List.of():files.stream().sorted(Comparator.comparing(FileRecovery::logicalPath)).toList();availableActions=availableActions==null?List.of():availableActions.stream().distinct().sorted().toList();issues=issues==null?List.of():issues.stream().distinct().sorted().toList();}
    public record FileRecovery(String logicalPath,MigrationRecoveryPlan.TargetState state,boolean rollbackAvailable,String reason){public FileRecovery{logicalPath=MigrationRunPlan.logical(logicalPath);Objects.requireNonNull(state);if(reason==null||reason.length()>512)throw new IllegalArgumentException("recovery reason");}}
    public enum Situation{NO_RECOVERY_REQUIRED,ABANDON_SAFE,GUARDED_ROLLBACK_AVAILABLE,GUARDED_ROLLBACK_PARTIALLY_BLOCKED,RECONCILE_METADATA_AVAILABLE,MANUAL_RECOVERY_REQUIRED}
}
