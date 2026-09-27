package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Read-only dry-run summary. Exact local-sensitive diffs are deliberately omitted. */
public record MigrationDryRunReport(String dryRunId,String checkpointRef,String gitSummary,
        int evidenceCount,Map<String,Integer>proposalsByEligibility,Map<String,Integer>proposalsByCategory,
        List<String>sourceTargets,int configurationOnlyProposals,int proposalsWithPatch,
        int conflictCount,int dependencyCount,List<String>requiredVerification,
        Map<String,String>existingDecisions,boolean eligibleForS11,List<String>limitations){
    public static MigrationDryRunReport create(String checkpoint,String gitSummary,List<MigrationEvidenceRef>evidence,
                                                MigrationProposalSet set,MigrationDecisionLedger ledger){Map<String,Integer>eligibility=new TreeMap<>(),categories=new TreeMap<>();List<String>targets=new ArrayList<>(),verification=new ArrayList<>(),limitations=new ArrayList<>(set.issues());int config=0,patch=0;for(var p:set.proposals()){eligibility.merge(p.eligibility().name(),1,Integer::sum);categories.merge(p.category().name(),1,Integer::sum);targets.addAll(p.sourceTargets());p.requiredVerification().forEach(v->verification.add(v.kind().name()));if(p.sourceEdits().isEmpty())config++;if(p.patchPreview()!=null)patch++;limitations.addAll(p.limitations());}Map<String,String>decisions=new TreeMap<>();if(ledger!=null)ledger.decisions().forEach(d->decisions.put(d.proposalId(),d.decision().name()));boolean s11=set.conflicts().isEmpty()&&set.completeness()==MigrationProposalSet.Completeness.COMPLETE&&set.proposals().stream().anyMatch(p->p.eligibility()==MigrationProposal.Eligibility.READY_FOR_REVIEW);List<String>semantic=new ArrayList<>(List.of(checkpoint,gitSummary,Integer.toString(evidence.size()),set.proposalSetId(),Boolean.toString(s11)));decisions.forEach((k,v)->{semantic.add(k);semantic.add(v);});String id="migration-dry-run-v1:sha256:"+MigrationDigests.digest("migration-dry-run-v1",semantic.toArray(String[]::new));return new MigrationDryRunReport(id,checkpoint,gitSummary,evidence.size(),eligibility,categories,targets.stream().distinct().sorted().toList(),config,patch,set.conflicts().size(),set.dependencies().size(),verification.stream().distinct().sorted().toList(),decisions,s11,limitations.stream().distinct().sorted().toList());}
}
