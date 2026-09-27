package io.github.testlens.migration.tooling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Immutable S10 proposal. It has no apply state and grants no source-write authority. */
public record MigrationProposal(int schemaVersion,int algorithmVersion,String proposalId,Category category,
        Eligibility eligibility,String problemCode,List<MigrationEvidenceRef> evidenceRefs,
        EvidenceCompleteness evidenceCompleteness,CausalState causalState,Confidence confidence,
        List<String> sourceTargets,List<String> declarationRefs,List<String> useSiteRefs,
        String currentSemantics,String proposedSemantics,List<MigrationSourcePrecondition> sourcePreconditions,
        List<MigrationSourceEdit> sourceEdits,PatchPreview patchPreview,BlastRadius blastRadius,
        List<VerificationStep> requiredVerification,RollbackPlan rollbackPlan,List<String> limitations,
        List<String> conflicts,List<String> proposalDependencies,String alreadyAppliedSemanticState) {
    public static final int SCHEMA_VERSION=1,ALGORITHM_VERSION=1,MAX_FILES=32,MAX_EDITS=128,MAX_USE_DETAILS=256;
    public MigrationProposal {
        if(schemaVersion!=1||algorithmVersion!=1)throw new IllegalArgumentException("proposal version");
        Objects.requireNonNull(category);Objects.requireNonNull(eligibility);Objects.requireNonNull(problemCode);
        evidenceRefs=sorted(evidenceRefs,Comparator.comparing(MigrationEvidenceRef::logicalArtifactRef));
        Objects.requireNonNull(evidenceCompleteness);Objects.requireNonNull(causalState);Objects.requireNonNull(confidence);
        sourceTargets=sortedStrings(sourceTargets);declarationRefs=sortedStrings(declarationRefs);useSiteRefs=sortedStrings(useSiteRefs);
        sourcePreconditions=List.copyOf(sourcePreconditions==null?List.of():sourcePreconditions);
        sourceEdits=List.copyOf(sourceEdits==null?List.of():sourceEdits);
        Objects.requireNonNull(blastRadius);requiredVerification=List.copyOf(requiredVerification==null?List.of():requiredVerification);
        Objects.requireNonNull(rollbackPlan);limitations=sortedStrings(limitations);conflicts=sortedStrings(conflicts);
        proposalDependencies=sortedStrings(proposalDependencies);
        if(sourceTargets.size()>MAX_FILES||sourceEdits.size()>MAX_EDITS||useSiteRefs.size()>MAX_USE_DETAILS)throw new IllegalArgumentException("proposal bounds");
        String expected=id(category,eligibility,problemCode,evidenceRefs,sourceTargets,declarationRefs,currentSemantics,
                proposedSemantics,sourcePreconditions,proposalDependencies);
        if(!expected.equals(proposalId))throw new IllegalArgumentException("proposalId");
    }
    public static String id(Category category,Eligibility eligibility,String problem,List<MigrationEvidenceRef>evidence,
                            List<String>targets,List<String>declarations,String before,String after,
                            List<MigrationSourcePrecondition>preconditions,List<String>dependencies){
        List<String> f=new ArrayList<>(List.of(category.name(),eligibility.name(),problem,n(before),n(after)));
        evidence.stream().sorted(Comparator.comparing(MigrationEvidenceRef::canonicalDigest)).forEach(x->{f.add(x.type().name());f.add(x.canonicalDigest());});
        targets.stream().sorted().forEach(f::add);declarations.stream().sorted().forEach(f::add);
        preconditions.stream().sorted(Comparator.comparing(MigrationSourcePrecondition::logicalPath)
                .thenComparingInt(x->x.exactCharacterRange().startUtf16())).forEach(x->{f.add(x.logicalPath());f.add(x.fileSha256());f.add(x.originalConstructDigest());f.add(x.proposedSemanticDigest());});
        dependencies.stream().sorted().forEach(f::add);
        return"migration-proposal-v1:sha256:"+MigrationDigests.digest("migration-proposal-v1",f.toArray(String[]::new));
    }
    public enum Category{CONFIGURATION,INTEGRATION,LOCATOR,INTERACTION,TEST_LOGIC,MANUAL_ONLY}
    public enum Eligibility{READY_FOR_REVIEW,REVIEW_REQUIRED,MANUAL_ONLY,BLOCKED_INSUFFICIENT_EVIDENCE,BLOCKED_STALE_INPUT,NO_CHANGE_RECOMMENDED}
    public enum EvidenceCompleteness{COMPLETE,PARTIAL,UNKNOWN}
    public enum CausalState{CONFIRMED,HYPOTHESIS,UNKNOWN}
    public enum Confidence{HIGH,MEDIUM,LOW,UNKNOWN}
    public enum UsageCoverage{COMPLETE,PARTIAL,UNKNOWN}
    public enum VerificationKind{COMPILE_AFFECTED_MODULE,RUN_DIRECTLY_AFFECTED_TEST,RUN_KNOWN_USE_SITE_TESTS,RUN_HEADED,RUN_HEADLESS,RUN_HEADLESS_FAST,CAPTURE_COMPATIBILITY_MANIFEST,COMPARE_COMPATIBILITY,MANUAL_REVIEW}
    public enum SideEffect{READ_ONLY,KNOWN_MUTATING,UNKNOWN}
    /** Metadata only. Exact diff bytes remain a separate local-sensitive artifact. */
    public record PatchPreview(String logicalRef,String digest,long byteCount){public PatchPreview{if(!digest.matches("sha256:[0-9a-f]{64}")||byteCount<0||byteCount>8L*1024*1024)throw new IllegalArgumentException("patch");Objects.requireNonNull(logicalRef);}}
    public record BlastRadius(int filesChanged,int declarationsChanged,int knownUseSiteCount,List<String>detailedUseSiteRefs,
                              List<String>potentiallyAffectedTests,boolean sharedDeclaration,UsageCoverage usageCoverage,
                              List<String>sourceCoverageIssues){public BlastRadius{detailedUseSiteRefs=sortedStrings(detailedUseSiteRefs);potentiallyAffectedTests=sortedStrings(potentiallyAffectedTests);sourceCoverageIssues=sortedStrings(sourceCoverageIssues);if(filesChanged>32||detailedUseSiteRefs.size()>256)throw new IllegalArgumentException("blast radius");}}
    public record VerificationStep(VerificationKind kind,SideEffect sideEffect,String subjectRef){public VerificationStep{Objects.requireNonNull(kind);Objects.requireNonNull(sideEffect);}}
    public record RollbackPlan(String kind,String preconditionDigest,String reverseSemanticRef){ }
    private static <T>List<T>sorted(List<T>v,Comparator<T>c){return(v==null?List.<T>of():v).stream().sorted(c).toList();}
    private static List<String>sortedStrings(List<String>v){return(v==null?List.<String>of():v).stream().distinct().sorted().toList();}
    private static String n(String v){return v==null?"":v;}
}
