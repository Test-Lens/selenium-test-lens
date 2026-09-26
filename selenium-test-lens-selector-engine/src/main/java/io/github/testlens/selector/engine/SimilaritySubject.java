package io.github.testlens.selector.engine;

import java.util.List;

/** Indexed view over an existing SelectorSubject; locator semantics stay in SelectorSubject. */
public record SimilaritySubject(SelectorSubject subject,String subjectRef,ComponentIdentity component,
                                String templateFingerprint,StructuralFamily structuralFamily,List<AppearanceSignal> appearanceSignals,
                                String exactDigest,List<String> explicitPatternRefs,List<String> candidateOrigins,
                                String semanticAttributeFamily,String usageSourceFamily,List<UsageReference> usages,
                                Correlation correlation,SourceState sourceState){
    public SimilaritySubject{appearanceSignals=List.copyOf(appearanceSignals);explicitPatternRefs=List.copyOf(explicitPatternRefs);candidateOrigins=List.copyOf(candidateOrigins);usages=List.copyOf(usages);if(subjectRef==null)subjectRef=ref(subject,component,templateFingerprint,exactDigest);}
    public static SimilaritySubject create(SelectorSubject subject,ComponentIdentity component,String normalizedTemplate,
                                           List<String> patternRefs,List<String> origins,String semanticFamily,String usageFamily,
                                           List<UsageReference> usages,Correlation correlation,SourceState sourceState){
        String template=normalizedTemplate==null?null:"selector-template-v1:sha256:"+CanonicalDigests.digest("selector-template-v1",normalizedTemplate);
        List<AppearanceSignal> signals=subject.hasTrustedExactValue()?new AppearanceClassifier().classify(subject):List.of();
        PatternProposal.Result proposal=new PatternProposal().propose(subject);
        StructuralFamily family=StructuralFamily.from(subject,component,proposal);
        String exact=subject.hasTrustedExactValue()?CanonicalDigests.exactValueDigest(subject):null;
        return new SimilaritySubject(subject,null,component,template,family,signals,exact,patternRefs,origins,semanticFamily,usageFamily,usages,correlation,sourceState);
    }
    private static String ref(SelectorSubject s,ComponentIdentity c,String template,String exact){return "selector-subject-v1:sha256:"+CanonicalDigests.digest("selector-subject-v1",n(s.declarationRef()),n(template),s.strategy(),n(exact),c.componentRef(),n(s.logicalPath()),n(s.contextFingerprint()));}
    private static String n(String v){return v==null?"":v;}
    public record UsageReference(String usageRef,String logicalPath,String usageClass,String usageMethod,Integer line){ }
    public record Correlation(Confidence confidence,List<String> supportingSignals,List<String> conflictingSignals,List<String> missingData,int algorithmVersion){public Correlation{supportingSignals=List.copyOf(supportingSignals);conflictingSignals=List.copyOf(conflictingSignals);missingData=List.copyOf(missingData);}}
    public enum Confidence{EXACT,STRONG,AMBIGUOUS,UNKNOWN}
    public enum SourceState{CURRENT,STALE_SOURCE_REVISION,UNRESOLVED_DECLARATION,CROSS_BUILD,UNKNOWN}
}
