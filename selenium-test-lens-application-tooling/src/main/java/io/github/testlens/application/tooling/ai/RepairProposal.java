package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Reviewable repair evidence. Tooling never silently applies a proposal. @since 0.5.0 */
public record RepairProposal(ContractHeader header,String proposalId,ApplicationPolicy applicationPolicy,String whatChanged,
                             String why,String oldSelector,String newSelector,List<String>sameTargetEvidence,
                             List<String>stabilityEvidence,List<String>affectedTests){public RepairProposal{if(header==null||applicationPolicy!=ApplicationPolicy.PROPOSE_ONLY)throw new IllegalArgumentException("Repairs must use PROPOSE_ONLY");sameTargetEvidence=canonical(sameTargetEvidence);stabilityEvidence=canonical(stabilityEvidence);affectedTests=canonical(affectedTests);}
    public enum ApplicationPolicy{PROPOSE_ONLY}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
