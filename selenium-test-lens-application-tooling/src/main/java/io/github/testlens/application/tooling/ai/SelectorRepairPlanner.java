package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.SourceImpact;

import java.util.List;
import java.util.Objects;

/** Converts already-ranked live Selector Intelligence evidence into a review-only source proposal. It never invents locators. @since 0.5.0 */
public final class SelectorRepairPlanner {
    public RepairProposal propose(String proposalId,FailureClassification classification,ApplicationModel.ElementModel before,
                                  ApplicationModel.ElementModel after,PageObjectCorrelation.ElementCorrelation correlation,
                                  SourceImpact impact,String classificationRef,String driftRef){
        Objects.requireNonNull(classification,"classification");Objects.requireNonNull(before,"before");Objects.requireNonNull(after,"after");Objects.requireNonNull(correlation,"correlation");Objects.requireNonNull(impact,"impact");
        if(classification.category()!=FailureClassification.Category.SELECTOR_INSTABILITY)throw new IllegalArgumentException("Selector repair requires SELECTOR_INSTABILITY classification");
        if(correlation.sourceDeclarationRef()==null||correlation.state()==PageObjectCorrelation.State.AMBIGUOUS||correlation.state()==PageObjectCorrelation.State.CONFLICT||correlation.state()==PageObjectCorrelation.State.NO_MATCH)throw new IllegalArgumentException("Selector repair requires an unambiguous source declaration correlation");
        var oldSelector=Objects.requireNonNull(before.preferredSelector(),"old preferred selector");var replacement=Objects.requireNonNull(after.preferredSelector(),"replacement preferred selector");
        if(replacement.source()!=ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS||!sameTarget(replacement)||!replacement.unique()||after.selectorQuality()!=ApplicationModel.SelectorQuality.VERIFIED)throw new IllegalArgumentException("Replacement must be live validated, same-target, unique, and verified by Selector Intelligence");
        if(oldSelector.candidateId().equals(replacement.candidateId())&&oldSelector.value().equals(replacement.value()))throw new IllegalArgumentException("Replacement must differ from the current selector");
        List<String>sameTarget=List.of("candidate="+replacement.candidateId(),"validation="+replacement.validation(),"sameTarget="+replacement.sameTarget(),"unique=true");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("Failure classification: SELECTOR_INSTABILITY","Source declaration correlation: "+correlation.state(),"Replacement selected by existing Selector Intelligence"),List.of(),ContractHeader.Confidence.LIVE_VALIDATED);
        return new RepairProposal(header,proposalId,RepairProposal.ApplicationPolicy.PROPOSE_ONLY,"Replace the correlated selector declaration","The old selector failed and the replacement is the live-ranked same target",oldSelector.strategy()+":"+oldSelector.value(),replacement.strategy()+":"+replacement.value(),correlation.sourceDeclarationRef(),oldSelector.candidateId(),replacement.candidateId(),classificationRef,driftRef,sameTarget,replacement.stability(),impact.testIds(),List.of("Source apply requires trusted host confirmation","Re-run every affected test"),List.of("Compile the affected module","Run affected tests: "+String.join(",",impact.testIds()),"Re-map and confirm same target"));
    }
    private static boolean sameTarget(ApplicationModel.SelectorProjection value){String target=value.sameTarget();return target!=null&&(target.equalsIgnoreCase("true")||target.toUpperCase(java.util.Locale.ROOT).contains("SAME_TARGET"));}
}
