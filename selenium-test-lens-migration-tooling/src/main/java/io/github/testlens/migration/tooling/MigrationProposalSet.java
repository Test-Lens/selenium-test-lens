package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Deterministic proposal set with explicit conflicts and dependencies. */
public record MigrationProposalSet(int schemaVersion,int algorithmVersion,String proposalSetId,String checkpointRef,String evidenceIndexRef,
        List<MigrationProposal>proposals,List<Conflict>conflicts,List<Dependency>dependencies,
        Completeness completeness,List<String>issues){
    public static final int MAX_PROPOSALS=10_000;
    public MigrationProposalSet{if(schemaVersion!=1||algorithmVersion!=1)throw new IllegalArgumentException("proposal set version");Objects.requireNonNull(checkpointRef);proposals=proposals.stream().sorted(Comparator.comparing((MigrationProposal p)->p.category().name()).thenComparing(p->p.sourceTargets().isEmpty()?"":p.sourceTargets().get(0)).thenComparing(MigrationProposal::proposalId)).toList();conflicts=conflicts.stream().sorted(Comparator.comparing(Conflict::code).thenComparing(x->String.join("",x.proposalIds()))).toList();dependencies=List.copyOf(dependencies);issues=issues.stream().distinct().sorted().toList();if(proposals.size()>MAX_PROPOSALS)throw new IllegalArgumentException("proposal bound");String expected=id(checkpointRef,evidenceIndexRef,proposals,conflicts,dependencies,completeness,issues);if(!expected.equals(proposalSetId))throw new IllegalArgumentException("proposalSetId");}
    public static MigrationProposalSet create(String checkpoint,String evidence,List<MigrationProposal>proposals,List<Dependency>dependencies,Completeness completeness,List<String>issues){List<Conflict>conflicts=detect(proposals);return new MigrationProposalSet(1,1,id(checkpoint,evidence,proposals,conflicts,dependencies,completeness,issues),checkpoint,evidence,proposals,conflicts,dependencies,completeness,issues);}
    public static List<Conflict>detect(List<MigrationProposal>proposals){List<Conflict>out=new ArrayList<>();for(int i=0;i<proposals.size();i++)for(int j=i+1;j<proposals.size();j++){MigrationProposal a=proposals.get(i),b=proposals.get(j);String code=null,ref=null;if(a.declarationRefs().stream().anyMatch(b.declarationRefs()::contains)&&!Objects.equals(a.proposedSemantics(),b.proposedSemantics())){code="SAME_DECLARATION_DIFFERENT_SEMANTICS";ref=a.declarationRefs().stream().filter(b.declarationRefs()::contains).findFirst().orElse(null);}else outer:for(var x:a.sourceEdits())for(var y:b.sourceEdits())if(x.logicalPath().equals(y.logicalPath())&&x.startUtf16()<y.endUtf16Exclusive()&&y.startUtf16()<x.endUtf16Exclusive()){code="OVERLAPPING_SOURCE_RANGES";ref=x.logicalPath();break outer;}if(code!=null)out.add(new Conflict(code,List.of(a.proposalId(),b.proposalId()),ref));}return out;}
    private static String id(String c,String e,List<MigrationProposal>p,List<Conflict>x,List<Dependency>d,Completeness completeness,List<String>issues){List<String>f=new ArrayList<>(List.of(c,e==null?"":e,completeness.name()));p.stream().map(MigrationProposal::proposalId).sorted().forEach(f::add);x.forEach(v->{f.add(v.code());v.proposalIds().stream().sorted().forEach(f::add);});d.forEach(v->{f.add(v.fromProposalId());f.add(v.code().name());f.add(v.toProposalId()==null?"":v.toProposalId());});issues.stream().sorted().forEach(f::add);return"migration-proposal-set-v1:sha256:"+MigrationDigests.digest("migration-proposal-set-v1",f.toArray(String[]::new));}
    public enum Completeness{COMPLETE,PARTIAL,INCOMPLETE}
    public enum DependencyCode{BLOCKED_BY_PROPOSAL,REQUIRES_NEW_EVIDENCE,REQUIRES_CONFIGURATION_ALIGNMENT,REQUIRES_MANUAL_DECISION}
    public record Conflict(String code,List<String>proposalIds,String sourceOrEvidenceRef){public Conflict{proposalIds=proposalIds.stream().sorted().toList();}}
    public record Dependency(String fromProposalId,DependencyCode code,String toProposalId){ }
}
