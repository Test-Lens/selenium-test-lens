package io.github.testlens.selector.engine;

import java.util.List;

public record SimilarityResult(int schemaVersion,int algorithmVersion,String sourceSubjectRef,QueryScope scope,
                               Completeness completeness,List<RelatedSubject> relatedSubjects){
    public static final int SCHEMA_VERSION=1,ALGORITHM_VERSION=1;
    public SimilarityResult{relatedSubjects=List.copyOf(relatedSubjects);}
    public enum QueryScope{SAME_DECLARATION,SAME_FILE,SAME_MODULE,PROJECT,SUPPLIED_HISTORY}
    public enum RelationClass{SAME_DECLARATION,EXACT_LOCATOR,SAME_TEMPLATE,EXPLICIT_PATTERN_RELATED,SAME_STRUCTURAL_FAMILY,APPEARANCE_RELATED,INSUFFICIENT_RELATION}
    public enum Signal{SAME_DECLARATION_REF,SAME_EXACT_DIGEST,SAME_TEMPLATE_FINGERPRINT,MATCHES_SAME_EXPLICIT_PATTERN,SAME_STRUCTURAL_FAMILY,SAME_PREFIX_FAMILY,SAME_APPEARANCE_FAMILY,SAME_STRATEGY,SAME_COMPONENT_REF,SAME_COMPONENT_ROLE,SAME_CONTEXT_FINGERPRINT,SAME_DECLARING_SYMBOL,SAME_LOGICAL_PATH,SAME_MODULE,SAME_USAGE_SOURCE_FAMILY,SAME_CANDIDATE_ORIGIN,SAME_SEMANTIC_ATTRIBUTE_FAMILY}
    public record Completeness(boolean complete,int catalogSubjects,int consideredSubjects,List<String> limitations){public Completeness{limitations=List.copyOf(limitations);}}
    public record RelatedSubject(String subjectRef,RelationClass relationClass,List<Signal> signals,
                                 SimilaritySubject.Confidence correlationConfidence,List<String> supportingSignals,
                                 List<String> conflictingSignals,String declarationRef,String logicalPath,
                                 List<SimilaritySubject.UsageReference> usageSamples,int usageCount,
                                 List<AppearanceSignal.Family> appearanceFamilies,StabilityAssessment policyAndEvidence,
                                 List<String> limitations,String explanation){public RelatedSubject{signals=List.copyOf(signals);supportingSignals=List.copyOf(supportingSignals);conflictingSignals=List.copyOf(conflictingSignals);usageSamples=List.copyOf(usageSamples);appearanceFamilies=List.copyOf(appearanceFamilies);limitations=List.copyOf(limitations);}}
}
