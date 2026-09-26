package io.github.testlens.selector.tooling;

import io.github.testlens.selector.engine.*;

import java.time.Instant;
import java.util.List;

final class SelectorHistoryModel {
    static final int SCHEMA_VERSION=1,FAMILY_VERSION=1,CORRELATION_VERSION=1;
    static final String GENERATOR_NAME="Test Lens selector history",GENERATOR_VERSION="0.4.0";
    static final String VALUE_UNAVAILABLE="VALUE_UNAVAILABLE";
    private SelectorHistoryModel(){}

    record Limits(int maxRuns,int maxSubjects,int maxBuckets,long maxSerializedBytes,int maxSamplesPerSubject,
                  int maxDistinctDigestsPerSubject,int maxUsageSamplesPerDeclaration){
        static Limits defaults(){return new Limits(100,25_000,100_000,64L*1024*1024,32,16,8);}
        Limits{if(maxRuns<1||maxSubjects<1||maxBuckets<1||maxSerializedBytes<1024||maxSamplesPerSubject<1||maxDistinctDigestsPerSubject<1||maxUsageSamplesPerDeclaration<1)throw new IllegalArgumentException("History limits must be positive");}
    }
    record History(int schemaVersion,int canonicalizationVersion,int detectorCatalogVersion,int familyFingerprintVersion,
                   int correlationAlgorithmVersion,Generator generator,Project project,List<Run> runs,List<Subject> subjects,
                   Coverage coverage,Retention retention){History{runs=List.copyOf(runs);subjects=List.copyOf(subjects);}}
    record Generator(String name,String version){}
    record Project(String projectFingerprint){}
    record Run(String runRef,String logicalTestIdentityRef,String framework,String testClass,String testMethod,
               String invocationDiscriminator,int attempt,String datasetKeyDigest,String testSourceRevision,
               String systemUnderTestRevision,String environmentKeyDigest,String sourceReportDigest,
               Instant observedAt,String chronologyKey,Long runSequence,boolean chronologyKnown){}
    record Subject(String historySubjectId,String strategy,String componentRole,List<String> componentPath,String componentRef,
                   String declarationRef,String templateFingerprint,String contextFingerprint,List<String> appearanceFamilies,
                   List<Family> structuralFamilies,List<ValueSummary> valueDigests,List<DomainSummary> domainSummaries,
                   UsageSummary usageSummary,List<Sample> samples,Comparability.ComparisonMode comparisonMode,
                   SimilaritySubject.Correlation correlation,SimilaritySubject.SourceState sourceState){Subject{componentPath=List.copyOf(componentPath);appearanceFamilies=List.copyOf(appearanceFamilies);structuralFamilies=List.copyOf(structuralFamilies);valueDigests=List.copyOf(valueDigests);domainSummaries=List.copyOf(domainSummaries);samples=List.copyOf(samples);}}
    record Family(String fingerprint,String prefixFingerprint,List<FamilySegment> segments){Family{segments=List.copyOf(segments);}}
    record FamilySegment(String kind,int length,String literalDigest){}
    record ValueSummary(String digest,int observationCount,int runCount){}
    record DomainSummary(ObservationEvidence.Domain domain,Comparability.State comparability,int comparableRuns,
                         int distinctValueDigests,boolean changed,List<String> reasonCodes){DomainSummary{reasonCodes=List.copyOf(reasonCodes);}}
    record UsageSummary(int count,List<SimilaritySubject.UsageReference> samples){UsageSummary{samples=List.copyOf(samples);}}
    record Sample(String sampleRef,String runRef,ObservationEvidence.Domain domain,String valueDigest,String valueState,
                  int occurrenceCount,String declarationRef,SimilaritySubject.Confidence correlationConfidence,
                  List<String> reasonCodes,boolean conflictExemplar){Sample{reasonCodes=List.copyOf(reasonCodes);}}
    record Coverage(int reportsImported,int reportsSkipped,int trustedObservations,int observationsAccepted,
                    int redactedObservations,int ambiguousCorrelations,int summaryOnlyRuns,boolean incomplete,List<String> issues){Coverage{issues=List.copyOf(issues);}}
    record Retention(int runsRetained,int subjectsRetained,int bucketsRetained,long serializedBytes,boolean pruned,
                     boolean chronologyIncomplete,List<String> issues){Retention{issues=List.copyOf(issues);}}

    record TrustedObservation(SelectorSubject subject,ComponentIdentity component,RunDescriptor run,
                              ObservationEvidence.Domain domain,Comparability.ComparisonMode comparisonMode,
                              String normalizedTemplate,String observationRef,SimilaritySubject.Correlation correlation,
                              SimilaritySubject.SourceState sourceState,SimilaritySubject.UsageReference usage,
                              boolean policyOrEvidenceConflict){ }
    record RedactedObservation(String strategy,ComponentIdentity component,RunDescriptor run,ObservationEvidence.Domain domain,
                               String observationRef,String declarationRef,String logicalPath,String usageClass,String usageMethod,
                               Integer usageLine,String outcome,Integer matchCount,String sourceReportDigest){ }
}
