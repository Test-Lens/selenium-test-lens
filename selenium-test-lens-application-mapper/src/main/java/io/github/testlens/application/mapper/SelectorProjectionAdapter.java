package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CandidateAnalysis;

import java.util.ArrayList;
import java.util.List;

final class SelectorProjectionAdapter {
    Projection project(CandidateAnalysis analysis,ApplicationModel.ElementType elementType,RedactionPolicy redaction){
        List<ApplicationModel.SelectorProjection>usable=new ArrayList<>();int position=0;
        for(CandidateAnalysis.Candidate candidate:analysis.candidates()){
            position++;if(candidate.locator()==null||unsafeValueProjection(candidate,elementType)||!usable(candidate.validation()))continue;
            String raw=candidate.locator().value(),safe=redaction.redact(raw);List<String>limitations=new ArrayList<>(candidate.limitations());
            if(!raw.equals(safe))limitations.add("SELECTOR_VALUE_REDACTED");
            List<String>stability=candidate.stabilityComponents().stream().map(x->x.assessment().effectiveDisposition().name()).distinct().sorted().toList();
            usable.add(new ApplicationModel.SelectorProjection(candidate.locator().strategy(),safe,candidate.candidateId(),candidate.validation().state().name(),candidate.validation().targetComparison().name(),candidate.validation().matchCount()==1,stability,position,candidate.reasons().stream().map(CandidateAnalysis.Reason::code).toList(),limitations,ApplicationModel.EvidenceSource.LIVE_CANDIDATE_ANALYSIS));
        }
        ApplicationModel.SelectorProjection preferred=usable.isEmpty()?null:usable.get(0);
        ApplicationModel.SelectorQuality quality=preferred==null?ApplicationModel.SelectorQuality.UNAVAILABLE:
                analysis.recommendation()==CandidateAnalysis.Recommendation.REVIEW_REQUIRED||preferred.limitations().contains("SELECTOR_VALUE_REDACTED")?ApplicationModel.SelectorQuality.REVIEW_REQUIRED:ApplicationModel.SelectorQuality.VERIFIED;
        return new Projection(preferred,usable.size()<2?List.of():usable.subList(1,usable.size()),quality);
    }
    Projection project(CandidateAnalysis analysis,RedactionPolicy redaction){return project(analysis,ApplicationModel.ElementType.UNKNOWN,redaction);}
    private static boolean unsafeValueProjection(CandidateAnalysis.Candidate candidate,ApplicationModel.ElementType elementType){return (elementType==ApplicationModel.ElementType.INPUT||elementType==ApplicationModel.ElementType.TEXTAREA)&&candidate.origins().contains(CandidateAnalysis.Origin.TEXT_XPATH);}
    private static boolean usable(CandidateAnalysis.Validation validation){return validation.targetComparison()==CandidateAnalysis.TargetComparison.SAME_TARGET&&(validation.state()==CandidateAnalysis.ValidationState.VERIFIED_IN_SCOPE||validation.state()==CandidateAnalysis.ValidationState.VALID_FOR_INTENT);}
    record Projection(ApplicationModel.SelectorProjection preferred,List<ApplicationModel.SelectorProjection>alternatives,ApplicationModel.SelectorQuality quality){}
}
