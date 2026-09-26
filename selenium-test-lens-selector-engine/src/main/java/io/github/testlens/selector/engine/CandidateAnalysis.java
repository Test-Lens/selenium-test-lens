package io.github.testlens.selector.engine;

import java.util.List;
import java.util.Objects;

/** Internal analysis schema. It deliberately contains no Selenium object identity. */
public record CandidateAnalysis(int schemaVersion, UsageIntent usageIntent, String contextFingerprint,
                                String declarationRef, String logicalPath, String originalCandidateId,
                                List<Candidate> candidates, Recommendation recommendation,
                                Completeness completeness, List<Issue> issues) {
    public static final int SCHEMA_VERSION=1;
    public CandidateAnalysis { candidates=List.copyOf(candidates);issues=List.copyOf(issues); }
    public enum UsageIntent { FIND_ONE, FIND_MANY, UNKNOWN }
    public enum Recommendation { KEEP_CURRENT, CONSIDER_REPLACEMENT, REVIEW_REQUIRED, NO_VALID_CANDIDATE, TARGET_REQUIRED }
    public record Completeness(boolean targetAvailable,boolean metadataTruncated,boolean candidateLimitReached,
                               int instrumentationNodesExcluded,int generatedBeforeDedup,int candidatesAfterDedup,
                               int validationCommands) { }
    public record Issue(String code,String detail,boolean fatal) { }
    public record Candidate(String candidateId,Locator locator,boolean opaqueOriginal,List<Origin> origins,
                            List<StabilityComponent> stabilityComponents,Validation validation,Complexity complexity,
                            List<Reason> reasons,List<String> limitations,boolean original) {
        public Candidate { Objects.requireNonNull(candidateId);origins=List.copyOf(origins);stabilityComponents=List.copyOf(stabilityComponents);Objects.requireNonNull(validation);Objects.requireNonNull(complexity);reasons=List.copyOf(reasons);limitations=List.copyOf(limitations);if(!opaqueOriginal&&locator==null)throw new IllegalArgumentException("scalar candidate requires locator"); }
        public Candidate withValidation(Validation v){return new Candidate(candidateId,locator,opaqueOriginal,origins,stabilityComponents,v,complexity,reasons,limitations,original);}
        public Candidate withReasons(List<Reason> v){return new Candidate(candidateId,locator,opaqueOriginal,origins,stabilityComponents,validation,complexity,v,limitations,original);}
    }
    public record Locator(String strategy,String value){public Locator{strategy=SelectorSubject.normalizeStrategy(strategy);Objects.requireNonNull(value);}}
    public enum Origin { ORIGINAL, ID, TEST_ATTRIBUTE, NAME, CLASS_TOKEN, TAG_CLASS, TAG, LINK_TEXT, TEXT_XPATH, STABLE_ANCESTOR, CUSTOM }
    public record StabilityComponent(String role,String strategy,String value,StabilityAssessment assessment) { }
    public record Validation(ValidationState state,int matchCount,TargetComparison targetComparison,List<String> issues){public Validation{issues=List.copyOf(issues);}public static Validation notLive(){return new Validation(ValidationState.NOT_LIVE_VALIDATED,-1,TargetComparison.UNKNOWN,List.of());}}
    public enum ValidationState { NOT_LIVE_VALIDATED, VERIFIED_IN_SCOPE, VALID_FOR_INTENT, VALID_BUT_AMBIGUOUS, WRONG_TARGET, NO_MATCH, INVALID_SELECTOR, STALE_TARGET, TARGET_UNAVAILABLE, CONTEXT_UNAVAILABLE, UNSUPPORTED, SESSION_LOST }
    public enum TargetComparison { SAME_TARGET, DIFFERENT_TARGET, TARGET_UNAVAILABLE, STALE_TARGET, UNKNOWN }
    public record Complexity(ScopeFragility scopeFragility,SemanticPreference semanticPreference,int ancestorDepth,int combinators,int locatorCodePoints,int componentCount) { }
    public enum ScopeFragility { DIRECT, SIMPLE_COMPOUND, ANCESTOR_COMPOUND, TEXT_XPATH }
    public enum SemanticPreference { PREFERRED_TEST_ATTRIBUTE, ID, NAME, CLASS_OR_TAG_CLASS, LINK_TEXT, ARIA_ATTRIBUTE, GENERIC_TEXT_OR_TAG }
    public record Reason(String code,String message) { }
}
