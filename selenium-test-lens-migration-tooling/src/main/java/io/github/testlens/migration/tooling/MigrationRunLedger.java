package io.github.testlens.migration.tooling;

import java.util.Comparator;
import java.util.List;

/** Local deterministic orchestration history. Timestamps live in individual operational results, not ledger identity. */
public record MigrationRunLedger(int schemaVersion,int algorithmVersion,String ledgerId,List<Entry>entries,OrchestrationState state,List<String>issues){
    public MigrationRunLedger{if(schemaVersion!=1||algorithmVersion!=1)throw new IllegalArgumentException("ledger version");entries=entries==null?List.of():entries.stream().sorted(Comparator.comparing(Entry::planId).thenComparing(Entry::runResultId)).toList();if(entries.size()>10_000)throw new IllegalArgumentException("ledger bound");issues=issues==null?List.of():issues.stream().distinct().sorted().toList();String expected=id(entries,state,issues);if(!expected.equals(ledgerId))throw new IllegalArgumentException("ledgerId");}
    public record Entry(String planId,String runResultId,MigrationRunPlan.BaselineRole baselineRole,String checkpointBefore,String checkpointAfter,List<String>artifactDigests,MigrationRunResult.Status status,List<String>issues){public Entry{artifactDigests=artifactDigests==null?List.of():artifactDigests.stream().sorted().toList();issues=issues==null?List.of():issues.stream().sorted().toList();}}
    public enum OrchestrationState{PREFLIGHT_READY,ISOLATED,BASELINE_READY,RUNNING,EVIDENCE_READY,ANALYSIS_READY,PROPOSALS_READY,REVIEWED_FOR_S11,INTERRUPTED,BLOCKED}
    public static MigrationRunLedger create(List<Entry>entries,OrchestrationState state,List<String>issues){return new MigrationRunLedger(1,1,id(entries,state,issues),entries,state,issues);}
    private static String id(List<Entry>entries,OrchestrationState state,List<String>issues){List<String>f=new java.util.ArrayList<>();entries.stream().sorted(Comparator.comparing(Entry::planId).thenComparing(Entry::runResultId)).forEach(e->{f.add(e.planId());f.add(e.runResultId());f.add(e.baselineRole().name());f.add(e.checkpointBefore());f.add(e.checkpointAfter()==null?"":e.checkpointAfter());f.add(e.status().name());f.addAll(e.artifactDigests());f.addAll(e.issues());});f.add(state.name());f.addAll(issues.stream().sorted().toList());return"migration-run-ledger-v1:sha256:"+MigrationDigests.digest("migration-run-ledger-v1",f.toArray(String[]::new));}
}
