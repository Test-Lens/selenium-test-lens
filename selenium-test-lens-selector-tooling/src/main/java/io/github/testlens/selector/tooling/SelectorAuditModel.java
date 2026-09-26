package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.AppearanceSignal;
import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.engine.SimilaritySubject;

import java.util.List;

/** Sanitized, deterministic selector-audit schema. Raw locator values never enter this model. */
final class SelectorAuditModel {
    static final int SCHEMA_VERSION=1,ALGORITHM_VERSION=1;
    static final int MAX_DECLARATIONS=100_000,MAX_RUNTIME_ONLY=25_000,MAX_FAMILIES=25_000;
    static final int MAX_FINDINGS=32,MAX_CANDIDATES=5,MAX_FAMILY_MEMBERS=100,MAX_SAMPLES=8;
    static final long MAX_SERIALIZED_BYTES=64L*1024*1024;
    private SelectorAuditModel(){}

    enum AnalysisMode{STATIC_ONLY,ENRICHED_OFFLINE}
    enum CompletenessStatus{COMPLETE_FOR_REQUESTED_INPUTS,PARTIAL,LIMITED}
    enum DimensionStatus{COMPLETE,PARTIAL,NOT_PROVIDED,UNSUPPORTED}
    enum Category{SOURCE,APPEARANCE,POLICY,EVIDENCE,VALIDATION,IMPROVEMENT,STRUCTURAL}
    enum FindingState{ACTIVE,COVERED_BY_POLICY,CONFLICTED,INFORMATIONAL,INCOMPLETE,UNSUPPORTED}
    enum Severity{INFO,REVIEW,WARNING,ERROR}
    enum Recommendation{NONE,KEEP_CURRENT,REVIEW,CONSIDER_REPLACEMENT,REVIEW_POLICY,COLLECT_MORE_EVIDENCE,LIVE_VALIDATE,MANUAL_REVIEW}
    enum SourceFreshness{CURRENT,STALE_INDEX,UNKNOWN}
    enum FindingCode{
        SOURCE_UNRESOLVED_DECLARATION,SOURCE_PARTIALLY_RESOLVED_DECLARATION,SOURCE_DYNAMIC_DECLARATION,SOURCE_CUSTOM_LOCATOR,SOURCE_UNSUPPORTED_DECLARATION,SOURCE_RESOLUTION_ERROR,
        APPEARANCE_GENERATED_LOOKING,
        POLICY_DECLARED_STABLE,POLICY_DECLARED_UNSTABLE,POLICY_POLICY_CONFLICT,POLICY_EVIDENCE_CONFLICT,
        EVIDENCE_OBSERVED_VARIABLE,EVIDENCE_NOT_AVAILABLE,EVIDENCE_HISTORY_INCOMPLETE,EVIDENCE_COMPARABILITY_UNKNOWN,EVIDENCE_RUNTIME_VALUE_REDACTED,EVIDENCE_AMBIGUOUS_CORRELATION,
        CURRENT_VERIFIED_IN_SCOPE,CURRENT_VALID_BUT_AMBIGUOUS,CURRENT_WRONG_TARGET,CURRENT_NO_MATCH,CURRENT_INVALID_SELECTOR,CURRENT_NOT_LIVE_VALIDATED,CURRENT_STALE_TARGET,CURRENT_TARGET_REQUIRED,
        CURRENT_LOCATOR_ALREADY_BEST,ALTERNATIVE_AVAILABLE,ALTERNATIVE_REQUIRES_REVIEW,NO_VALID_ALTERNATIVE,
        DUPLICATE_EXACT_LOCATOR,RELATED_GENERATED_FAMILY,BROAD_PATTERN_IMPACT,USAGE_SCOPE_UNKNOWN
    }
    enum ProjectIssueCode{STATIC_COVERAGE_INCOMPLETE,UNSUPPORTED_SOURCE_LANGUAGE,INCOMPLETE_CLASSPATH,MALFORMED_REQUIRED_SELECTOR_INDEX,MALFORMED_OPTIONAL_HISTORY,MALFORMED_OPTIONAL_POLICY,CANDIDATE_INPUT_REJECTED,PROJECT_FINGERPRINT_MISMATCH,STALE_SELECTOR_INDEX,ORPHANED_POLICY_RULE,ORPHANED_HISTORY_SUBJECT,UNSUPPORTED_INPUT_VERSION,SUMMARY_ONLY_HISTORY,HISTORY_PRUNED,OUTPUT_TRUNCATED,OUTPUT_LIMIT_EXCEEDED,STATIC_ANALYSIS_INTERNAL_ERROR}

    record AuditReport(int auditSchemaVersion,int auditAlgorithmVersion,EngineVersions engineVersions,Project project,
                       GeneratedFrom generatedFrom,AnalysisMode analysisMode,CompletenessStatus overallCompleteness,
                       Summary summary,Coverage coverage,List<ProjectIssue> projectIssues,List<DeclarationAudit> declarations,
                       List<RuntimeOnlyAudit> runtimeOnlySubjects,List<FamilyAudit> families,List<String> outputLimitations){
        AuditReport{projectIssues=List.copyOf(projectIssues);declarations=List.copyOf(declarations);runtimeOnlySubjects=List.copyOf(runtimeOnlySubjects);families=List.copyOf(families);outputLimitations=List.copyOf(outputLimitations);}
    }
    record EngineVersions(String auditVersion,String selectorEngineVersion,int detectorCatalogVersion,int canonicalizationVersion,
                          Integer historySchemaVersion,Integer familyFingerprintVersion,Integer correlationAlgorithmVersion,
                          Integer candidateSchemaVersion,List<String> limitations){EngineVersions{limitations=List.copyOf(limitations);}}
    record Project(String projectFingerprint,String sourceRevision,List<String> sourceRoots){Project{sourceRoots=List.copyOf(sourceRoots);}}
    record GeneratedFrom(String selectorIndexFingerprint,String historyFingerprint,String policyFingerprint,int candidateAnalyses){ }
    record Summary(int declarationsAudited,int runtimeOnlySubjects,int activeReviewFindings,int warnings,int errors,int conflicts,
                   int observedVariable,int generatedLooking,int generatedLookingCoveredByPolicy,int liveValidatedDeclarations,
                   int alternativesAvailable,int unresolvedOrUnsupported,int historyIncomplete,int orphanedPolicies,int families){ }
    record Coverage(DimensionCoverage staticCoverage,DimensionCoverage runtimeCoverage,DimensionCoverage historyCoverage,
                    DimensionCoverage policyCoverage,DimensionCoverage candidateCoverage,DimensionCoverage outputCoverage){ }
    record DimensionCoverage(DimensionStatus status,List<Count> counts,List<String> limitations){DimensionCoverage{counts=List.copyOf(counts);limitations=List.copyOf(limitations);}}
    record Count(String name,long value){ }
    record ProjectIssue(String issueId,ProjectIssueCode code,Severity severity,List<String> reasonCodes,List<String> references,String explanation){ProjectIssue{reasonCodes=List.copyOf(reasonCodes);references=List.copyOf(references);}}

    record DeclarationAudit(String declarationRef,SourceRef source,String declarationKind,String resolutionStatus,String expressionKind,
                            String expressionDigest,SourceFreshness freshness,List<ComponentAudit> components,CorrelationSummary correlationSummary,
                            UsageSummary usageSummary,List<AuditFinding> findings,PolicySummary policySummary,EvidenceSummary evidenceSummary,
                            CandidateSummary candidateSummary,SimilarSummary similarSummary,Recommendation primaryRecommendation,
                            List<Recommendation> supportingActions,List<String> limitations){
        DeclarationAudit{components=List.copyOf(components);findings=List.copyOf(findings);supportingActions=List.copyOf(supportingActions);limitations=List.copyOf(limitations);}
    }
    record SourceRef(String logicalPath,int startLine,int startColumn,int endLine,int endColumn,int startOffset,int endOffsetExclusive,boolean generated,boolean readOnly){ }
    record ComponentAudit(String componentRef,String componentRole,List<String> componentPath,String strategy,String exactDigest,
                          List<AppearanceSignal.Family> appearanceFamilies,String structuralFamilyFingerprint,
                          String effectiveDisposition){ComponentAudit{componentPath=List.copyOf(componentPath);appearanceFamilies=List.copyOf(appearanceFamilies);}}
    record CorrelationSummary(SimilaritySubject.Confidence strongestConfidence,int exact,int strong,int ambiguous,int unknown,List<String> limitations){CorrelationSummary{limitations=List.copyOf(limitations);}}
    record UsageSummary(int count,List<UsageSample> samples,boolean truncated){UsageSummary{samples=List.copyOf(samples);}}
    record UsageSample(String usageRef,String logicalPath,String usageClassDigest,String usageMethodDigest,Integer line){ }
    record PolicySummary(List<String> matchedRuleIds,List<String> selectedRuleIds,List<String> decisions,int conflictCount){PolicySummary{matchedRuleIds=List.copyOf(matchedRuleIds);selectedRuleIds=List.copyOf(selectedRuleIds);decisions=List.copyOf(decisions);}}
    record EvidenceSummary(String state,int comparableDomains,int changedDomains,int distinctValueDigests,boolean incomplete,List<String> domains,List<String> limitations){EvidenceSummary{domains=List.copyOf(domains);limitations=List.copyOf(limitations);}}
    record CandidateSummary(String originalCandidateId,String recommendation,String originalValidationState,int totalCandidateCount,
                            List<CandidateDetail> alternatives,boolean candidatesTruncated,List<String> limitations){CandidateSummary{alternatives=List.copyOf(alternatives);limitations=List.copyOf(limitations);}}
    record CandidateDetail(String candidateId,List<String> origins,String validationState,int matchCount,List<String> reasonCodes,List<String> limitations){CandidateDetail{origins=List.copyOf(origins);reasonCodes=List.copyOf(reasonCodes);limitations=List.copyOf(limitations);}}
    record SimilarSummary(List<String> familyFingerprints,int relatedCount,List<String> patternProposalKinds,int previewMatches){SimilarSummary{familyFingerprints=List.copyOf(familyFingerprints);patternProposalKinds=List.copyOf(patternProposalKinds);}}

    record AuditFinding(String findingId,Category category,FindingCode code,Severity severity,FindingState state,String componentRef,
                        List<String> reasonCodes,List<String> evidenceRefs,List<String> policyRuleIds,List<String> candidateIds,
                        List<String> familyFingerprints,String explanation){AuditFinding{reasonCodes=List.copyOf(reasonCodes);evidenceRefs=List.copyOf(evidenceRefs);policyRuleIds=List.copyOf(policyRuleIds);candidateIds=List.copyOf(candidateIds);familyFingerprints=List.copyOf(familyFingerprints);}}
    record RuntimeOnlyAudit(String subjectRef,SimilaritySubject.Confidence correlationConfidence,List<String> supportingSignals,
                            List<String> conflictingSignals,List<AuditFinding> findings,EvidenceSummary evidenceSummary,
                            UsageSummary usageSummary,List<String> limitations){RuntimeOnlyAudit{supportingSignals=List.copyOf(supportingSignals);conflictingSignals=List.copyOf(conflictingSignals);findings=List.copyOf(findings);limitations=List.copyOf(limitations);}}
    record FamilyAudit(String familyFingerprint,String relationClass,int declarationCount,int runtimeHistorySubjectCount,
                       List<String> modules,List<String> paths,List<AppearanceSignal.Family> appearanceFamilies,int policyCoveredMembers,
                       int evidenceConflicts,PatternSummary patternProposal,List<String> memberRefs,int totalMembers,
                       boolean membersTruncated){FamilyAudit{modules=List.copyOf(modules);paths=List.copyOf(paths);appearanceFamilies=List.copyOf(appearanceFamilies);memberRefs=List.copyOf(memberRefs);}}
    record PatternSummary(List<String> segmentKinds,int matchedStatic,int matchedHistory,int excluded,int conflicts,List<String> affectedDeclarationRefs){PatternSummary{segmentKinds=List.copyOf(segmentKinds);affectedDeclarationRefs=List.copyOf(affectedDeclarationRefs);}}
}
