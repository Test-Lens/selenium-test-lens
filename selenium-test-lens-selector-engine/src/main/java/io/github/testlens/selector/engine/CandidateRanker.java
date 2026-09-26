package io.github.testlens.selector.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;

/** Lexicographic, score-free ranking and advisory recommendation. */
public final class CandidateRanker {
    public List<Candidate> rank(List<Candidate> input){return input.stream().sorted(comparator()).map(this::withReasons).toList();}
    public Recommendation recommend(List<Candidate> ranked,String originalId,boolean targetAvailable){
        if(!targetAvailable)return Recommendation.TARGET_REQUIRED;
        Optional<Candidate> usable=ranked.stream().filter(c->usable(c.validation())).findFirst();
        if(usable.isEmpty())return ranked.stream().anyMatch(CandidateRanker::reviewableValidation)
                ?Recommendation.REVIEW_REQUIRED:Recommendation.NO_VALID_CANDIDATE;
        Candidate top=usable.get();if(review(top))return Recommendation.REVIEW_REQUIRED;
        Candidate original=ranked.stream().filter(c->Objects.equals(c.candidateId(),originalId)).findFirst().orElse(null);
        if(original==null)return Recommendation.CONSIDER_REPLACEMENT;
        if(top.candidateId().equals(originalId)||!materiallyBetter(top,original))return Recommendation.KEEP_CURRENT;
        return Recommendation.CONSIDER_REPLACEMENT;
    }
    private Candidate withReasons(Candidate c){List<Reason> reasons=new ArrayList<>(c.reasons());switch(c.validation().state()){
        case VERIFIED_IN_SCOPE->reasons.add(new Reason("UNIQUE_SAME_TARGET","Unique match for the target in the current live context"));
        case VALID_FOR_INTENT->reasons.add(new Reason("VALID_FOR_USAGE_INTENT","Matches the supplied usage intent and contains the target"));
        case VALID_BUT_AMBIGUOUS->reasons.add(new Reason("AMBIGUOUS_FOR_FIND_ONE","Multiple matches include the target"));
        case WRONG_TARGET->reasons.add(new Reason("WRONG_TARGET","Resolved matches do not identify the supplied target"));
        case NO_MATCH->reasons.add(new Reason("NO_MATCH","Candidate resolved no elements"));
        case INVALID_SELECTOR->reasons.add(new Reason("INVALID_SELECTOR","Selenium rejected the selector"));default->{}}
        for(StabilityComponent component:c.stabilityComponents())switch(component.assessment().effectiveDisposition()){
            case DECLARED_STABLE->reasons.add(new Reason("POLICY_EXPLICIT_STABLE","Project policy declares this component stable"));
            case DECLARED_UNSTABLE->reasons.add(new Reason("POLICY_EXPLICIT_UNSTABLE","Project policy declares this component unstable"));
            case REVIEW_GENERATED_LOOKING->reasons.add(new Reason("GENERATED_LOOKING","Generated-looking appearance requires review"));
            case POLICY_EVIDENCE_CONFLICT->reasons.add(new Reason("POLICY_EVIDENCE_CONFLICT","Declared stability conflicts with observed variability"));default->{}}
        if(c.original())reasons.add(new Reason("CURRENT_LOCATOR_ALREADY_BEST","Original locator participates in churn avoidance"));
        return c.withReasons(reasons);
    }
    private static Comparator<Candidate> comparator(){return Comparator
            .comparingInt((Candidate c)->live(c.validation().state())).reversed()
            .thenComparing(Comparator.comparingInt((Candidate c)->stability(c.stabilityComponents())).reversed())
            .thenComparingInt(c->c.complexity().scopeFragility().ordinal())
            .thenComparingInt(c->c.complexity().semanticPreference().ordinal())
            .thenComparingInt(c->c.complexity().ancestorDepth()).thenComparingInt(c->c.complexity().combinators())
            .thenComparingInt(c->c.complexity().locatorCodePoints()).thenComparingInt(c->c.complexity().componentCount())
            .thenComparing((Candidate c)->!c.original())
            .thenComparing(c->c.locator()==null?"~":c.locator().strategy())
            .thenComparing(c->c.locator()==null?"~":c.locator().value()).thenComparing(Candidate::candidateId);}
    private static int live(ValidationState state){return switch(state){case VERIFIED_IN_SCOPE->10;case VALID_FOR_INTENT->9;case VALID_BUT_AMBIGUOUS->8;case NOT_LIVE_VALIDATED->7;case TARGET_UNAVAILABLE,CONTEXT_UNAVAILABLE,STALE_TARGET,UNSUPPORTED->6;case NO_MATCH,INVALID_SELECTOR->2;case WRONG_TARGET->1;case SESSION_LOST->0;};}
    private static int stability(List<StabilityComponent> components){return components.stream().map(x->x.assessment().effectiveDisposition()).mapToInt(d->switch(d){case DECLARED_STABLE->7;case NO_APPEARANCE_SIGNAL->6;case INSUFFICIENT_DATA->5;case REVIEW_GENERATED_LOOKING->4;case POLICY_POLICY_CONFLICT,POLICY_EVIDENCE_CONFLICT->3;case OBSERVED_VARIABLE->2;case DECLARED_UNSTABLE->1;}).min().orElse(5);}
    private static boolean usable(Validation validation){return validation.state()==ValidationState.VERIFIED_IN_SCOPE||validation.state()==ValidationState.VALID_FOR_INTENT;}
    private static boolean reviewableValidation(Candidate candidate){return switch(candidate.validation().state()){case VALID_BUT_AMBIGUOUS,NOT_LIVE_VALIDATED,STALE_TARGET,TARGET_UNAVAILABLE,CONTEXT_UNAVAILABLE,UNSUPPORTED->true;default->false;};}
    private static boolean review(Candidate candidate){return candidate.stabilityComponents().stream().anyMatch(x->switch(x.assessment().effectiveDisposition()){case REVIEW_GENERATED_LOOKING,POLICY_POLICY_CONFLICT,POLICY_EVIDENCE_CONFLICT,OBSERVED_VARIABLE,DECLARED_UNSTABLE->true;default->false;});}
    private static boolean materiallyBetter(Candidate a,Candidate b){return live(a.validation().state())>live(b.validation().state())||stability(a.stabilityComponents())>stability(b.stabilityComponents())||a.complexity().scopeFragility().ordinal()<b.complexity().scopeFragility().ordinal()||a.complexity().semanticPreference().ordinal()<b.complexity().semanticPreference().ordinal();}
}
