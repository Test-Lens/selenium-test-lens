package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.application.tooling.source.SourceImpact;
import io.github.testlens.selector.tooling.ExistingProjectIndex;
import io.github.testlens.selector.tooling.ExistingProjectIndexer;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Converts already-ranked live Selector Intelligence evidence into a review-only source proposal. It never invents locators. @since 0.5.0 */
public final class SelectorRepairPlanner {
    public RepairProposal propose(String proposalId,FailureClassification classification,ApplicationModel.ElementModel before,
                                  ApplicationModel.ElementModel after,PageObjectCorrelation.ElementCorrelation correlation,
                                  SourceImpact impact,String classificationRef,String driftRef){
        Evidence evidence=validate(classification,before,after,correlation,impact);
        return proposal(proposalId,evidence,correlation,impact,classificationRef,driftRef,null);
    }

    /** Builds a proposal with the exact source preconditions required by trusted apply. */
    public RepairProposal propose(String proposalId,FailureClassification classification,ApplicationModel.ElementModel before,
                                  ApplicationModel.ElementModel after,PageObjectCorrelation.ElementCorrelation correlation,
                                  SourceImpact impact,ExistingProjectIndex index,String classificationRef,String driftRef){
        Evidence evidence=validate(classification,before,after,correlation,impact);
        Objects.requireNonNull(index,"index");
        ExistingProjectIndex.ElementEntry declaration=index.elements().stream()
                .filter(value->value.id().equals(correlation.sourceElementId()))
                .filter(value->value.declarationRef().equals(correlation.sourceDeclarationRef()))
                .findFirst().orElseThrow(()->new IllegalArgumentException("Correlated source declaration is absent from the current index"));
        ExistingProjectIndex.ClassEntry owner=index.classes().stream()
                .filter(value->value.id().equals(declaration.ownerClassId())).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Correlated declaration owner is absent from the current index"));
        ExistingProjectIndex.SourceFile sourceFile=index.sourceFiles().stream()
                .filter(value->value.logicalPath().equals(owner.logicalPath())).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Correlated source file is absent from the current index"));
        if(declaration.range()==null||declaration.declarationFingerprint()==null){
            throw new IllegalArgumentException("Correlated declaration lacks source apply provenance");
        }
        if(!normalize(declaration.strategy()).equals(normalize(evidence.oldSelector().strategy()))
                ||declaration.valueProjection()==null
                ||!Objects.equals(declaration.valueProjection().fingerprint(),
                ExistingProjectIndexer.selectorValueFingerprint(evidence.oldSelector().value()))){
            throw new IllegalArgumentException("Indexed declaration does not match the old selector");
        }
        var range=declaration.range();
        RepairProposal.SourceTarget sourceTarget=new RepairProposal.SourceTarget(declaration.id(),
                declaration.declarationRef(),owner.logicalPath(),new RepairProposal.SourceRange(range.startLine(),
                range.startColumn(),range.endLine(),range.endColumn(),range.startOffset(),range.endOffsetExclusive()),
                sourceFile.contentFingerprint(),declaration.declarationFingerprint(),correlation.state().name(),
                evidence.oldSelector().strategy(),evidence.oldSelector().value());
        return proposal(proposalId,evidence,correlation,impact,classificationRef,driftRef,sourceTarget);
    }

    private static Evidence validate(FailureClassification classification,ApplicationModel.ElementModel before,
                                     ApplicationModel.ElementModel after,
                                     PageObjectCorrelation.ElementCorrelation correlation,SourceImpact impact){
        Objects.requireNonNull(classification,"classification");Objects.requireNonNull(before,"before");Objects.requireNonNull(after,"after");Objects.requireNonNull(correlation,"correlation");Objects.requireNonNull(impact,"impact");
        if(classification.category()!=FailureClassification.Category.SELECTOR_INSTABILITY)throw new IllegalArgumentException("Selector repair requires SELECTOR_INSTABILITY classification");
        if(correlation.sourceDeclarationRef()==null||(correlation.state()!=PageObjectCorrelation.State.EXACT&&correlation.state()!=PageObjectCorrelation.State.STRONG))throw new IllegalArgumentException("Selector repair requires an EXACT or STRONG source declaration correlation");
        var oldSelector=Objects.requireNonNull(before.preferredSelector(),"old preferred selector");var replacement=Objects.requireNonNull(after.preferredSelector(),"replacement preferred selector");
        if(replacement.source()!=ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS
                ||!"VERIFIED_IN_SCOPE".equalsIgnoreCase(replacement.validation())
                ||!"SAME_TARGET".equalsIgnoreCase(replacement.sameTarget())||!replacement.unique()
                ||after.selectorQuality()!=ApplicationModel.SelectorQuality.VERIFIED||!acceptable(replacement.stability())){
            throw new IllegalArgumentException("Replacement must be live VERIFIED_IN_SCOPE, SAME_TARGET, unique, and stability-acceptable");
        }
        if(oldSelector.candidateId().equals(replacement.candidateId())&&oldSelector.value().equals(replacement.value()))throw new IllegalArgumentException("Replacement must differ from the current selector");
        return new Evidence(oldSelector,replacement);
    }

    private static RepairProposal proposal(String proposalId,Evidence evidence,
                                            PageObjectCorrelation.ElementCorrelation correlation,
                                            SourceImpact impact,String classificationRef,String driftRef,
                                            RepairProposal.SourceTarget sourceTarget){
        var oldSelector=evidence.oldSelector();var replacement=evidence.replacement();
        List<String>sameTarget=List.of("candidate="+replacement.candidateId(),"validation="+replacement.validation(),"sameTarget="+replacement.sameTarget(),"unique=true");
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,ContractHeader.Status.READY,List.of("Failure classification: SELECTOR_INSTABILITY","Source declaration correlation: "+correlation.state(),"Replacement selected by existing Selector Intelligence"),List.of(),ContractHeader.Confidence.LIVE_VALIDATED);
        RepairProposal.SelectorEvidence replacementEvidence=new RepairProposal.SelectorEvidence(replacement.strategy(),
                replacement.value(),replacement.candidateId(),replacement.validation(),replacement.sameTarget(),
                replacement.unique(),replacement.stability(),replacement.source().name());
        return new RepairProposal(header,proposalId,RepairProposal.ApplicationPolicy.PROPOSE_ONLY,
                "Replace the correlated selector declaration",
                "The old selector failed and the replacement is the live-ranked same target",
                oldSelector.strategy()+":"+oldSelector.value(),replacement.strategy()+":"+replacement.value(),
                correlation.sourceDeclarationRef(),oldSelector.candidateId(),replacement.candidateId(),
                classificationRef,driftRef,sameTarget,replacement.stability(),impact.testIds(),
                List.of("Source apply requires trusted host confirmation","Re-run every affected test"),
                List.of("Compile the affected module","Run affected tests: "+String.join(",",impact.testIds()),
                        "Re-map and confirm same target"),sourceTarget,replacementEvidence,impact.methodIds());
    }

    private static boolean acceptable(List<String> stability){
        return !stability.isEmpty()&&stability.stream().map(value->value.toUpperCase(Locale.ROOT)).allMatch(value->
                value.equals("NO_APPEARANCE_SIGNAL")||value.equals("DECLARED_STABLE")||value.equals("STABLE"));
    }

    private static String normalize(String value){
        return Objects.toString(value,"").toLowerCase(Locale.ROOT).replace("by.","")
                .replace("selector","").replace("_","").replace("-","").trim();
    }

    private record Evidence(ApplicationModel.SelectorProjection oldSelector,
                            ApplicationModel.SelectorProjection replacement){}
}
