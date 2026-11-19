package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded, redacted task context; it deliberately contains projections rather than the complete model. @since 0.5.0 */
public record AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,
                               Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,
                               SourceContext source,PageObjectCapabilityManifest pageObjectCapabilities,
                               List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness) {
    public AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness){this(header,task,pages,pageObjectApis,existingTestConventions,SourceContext.empty(),PageObjectCapabilityManifest.empty(),included,excluded,completeness);}
    public AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,SourceContext source,List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness){this(header,task,pages,pageObjectApis,existingTestConventions,source,PageObjectCapabilityManifest.empty(),included,excluded,completeness);}
    public AgentContextPack{if(header==null||task==null||completeness==null)throw new IllegalArgumentException("header, task and completeness are required");pages=(pages==null?List.<PageContext>of():pages).stream().sorted(Comparator.comparing(PageContext::pageId)).toList();var map=new TreeMap<String,List<String>>();if(pageObjectApis!=null)pageObjectApis.forEach((key,value)->map.put(key,canonical(value)));pageObjectApis=Map.copyOf(map);existingTestConventions=canonical(existingTestConventions);source=source==null?SourceContext.empty():source;pageObjectCapabilities=pageObjectCapabilities==null?PageObjectCapabilityManifest.empty():pageObjectCapabilities;included=decisions(included);excluded=decisions(excluded);}
    public record PageContext(String pageId,String canonicalName,String urlPattern,List<StateContext>states,List<ElementContext>elements,List<TransitionContext>transitions){public PageContext{states=(states==null?List.<StateContext>of():states).stream().sorted(Comparator.comparing(StateContext::stateId)).toList();elements=(elements==null?List.<ElementContext>of():elements).stream().sorted(Comparator.comparing(ElementContext::elementId)).toList();transitions=(transitions==null?List.<TransitionContext>of():transitions).stream().sorted(Comparator.comparing(TransitionContext::transitionId)).toList();}}
    public record StateContext(String stateId,String semanticName,List<String>elementIds){public StateContext{elementIds=canonical(elementIds);}}
    public record ElementContext(String elementId,String semanticName,String type,List<String>actions,String selectorQuality){public ElementContext{actions=canonical(actions);}}
    public record TransitionContext(String transitionId,String action,String elementId,String targetPageId,String targetStateId,String confidence){}
    public record ScopeDecision(String id,String kind,String reason){}
    public record SourceContext(List<PageObjectClassContext>classes,List<PageObjectMethodContext>methods,
                                List<SourceDeclarationContext>declarations,List<ExistingTestContext>tests){
        public SourceContext{classes=sorted(classes,Comparator.comparing(PageObjectClassContext::classId));methods=sorted(methods,Comparator.comparing(PageObjectMethodContext::methodId));declarations=sorted(declarations,Comparator.comparing(SourceDeclarationContext::declarationRef));tests=sorted(tests,Comparator.comparing(ExistingTestContext::testId));}
        public static SourceContext empty(){return new SourceContext(List.of(),List.of(),List.of(),List.of());}
    }
    public record PageObjectClassContext(String classId,String pageId,String qualifiedName,String logicalPath,String classification,String origin,List<String>includedBecause){public PageObjectClassContext{includedBecause=canonical(includedBecause);}}
    public record PageObjectMethodContext(String methodId,String ownerClassId,String signature,String classification,List<String>actions,List<String>referencedApplicationElementIds,List<String>outgoingTypes,List<String>includedBecause){public PageObjectMethodContext{actions=canonical(actions);referencedApplicationElementIds=canonical(referencedApplicationElementIds);outgoingTypes=canonical(outgoingTypes);includedBecause=canonical(includedBecause);}}
    public record SourceDeclarationContext(String declarationRef,String ownerClassId,String fieldName,String strategy,String selectorValueFingerprint,String applicationElementId,String correlationState,List<String>includedBecause){public SourceDeclarationContext{includedBecause=canonical(includedBecause);}}
    public record ExistingTestContext(String testId,String ownerClassId,String methodId,String framework,List<String>tags,List<String>groups,List<String>relevantMethodIds,List<String>includedBecause){public ExistingTestContext{tags=canonical(tags);groups=canonical(groups);relevantMethodIds=canonical(relevantMethodIds);includedBecause=canonical(includedBecause);}}
    public record PageObjectCapabilityManifest(String sourceFingerprint,ManifestCompleteness completeness,
                                                List<PageObjectCapability>classes,List<String>limitations,
                                                ManifestProvenance provenance){
        public PageObjectCapabilityManifest(String sourceFingerprint,ManifestCompleteness completeness,
                                             List<PageObjectCapability>classes,List<String>limitations){
            this(sourceFingerprint,completeness,classes,limitations,
                    ManifestProvenance.unspecified(sourceFingerprint,completeness));
        }
        public PageObjectCapabilityManifest{if(completeness==null)throw new IllegalArgumentException("manifest completeness is required");classes=sorted(classes,Comparator.comparing(PageObjectCapability::qualifiedName).thenComparing(PageObjectCapability::classId));limitations=canonical(limitations);provenance=provenance==null?ManifestProvenance.unspecified(sourceFingerprint,completeness):provenance;}
        public static PageObjectCapabilityManifest empty(){return new PageObjectCapabilityManifest(null,ManifestCompleteness.PARTIAL,List.of(),List.of("Capability manifest was not supplied"),ManifestProvenance.empty());}
    }
    public record ManifestProvenance(String indexFingerprint,ManifestOriginStage originStage,
                                     String sourceIndexCompleteness,int selectedPageObjectCount,
                                     int verifiedClassCount,int verifiedMethodCount,int unresolvedClassCount,
                                     ManifestConstructionStatus constructionStatus,
                                     ManifestInheritedApiStatus inheritedApiStatus,
                                     ManifestCorrelationStatus correlationStatus,boolean truncated,
                                     int maxClasses,int maxMethods,int maxSerializedCharacters,
                                     boolean sourcesChanged,List<ManifestPartialReason>partialReasons){
        public ManifestProvenance{
            originStage=originStage==null?ManifestOriginStage.UNSPECIFIED:originStage;
            sourceIndexCompleteness=sourceIndexCompleteness==null?"UNKNOWN":sourceIndexCompleteness;
            constructionStatus=constructionStatus==null?ManifestConstructionStatus.UNRESOLVED:constructionStatus;
            inheritedApiStatus=inheritedApiStatus==null?ManifestInheritedApiStatus.UNRESOLVED:inheritedApiStatus;
            correlationStatus=correlationStatus==null?ManifestCorrelationStatus.INSUFFICIENT:correlationStatus;
            partialReasons=(partialReasons==null?List.<ManifestPartialReason>of():partialReasons).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();
            if(selectedPageObjectCount<0||verifiedClassCount<0||verifiedMethodCount<0||unresolvedClassCount<0
                    ||maxClasses<0||maxMethods<0||maxSerializedCharacters<0)throw new IllegalArgumentException("manifest provenance counts must be non-negative");
        }
        public static ManifestProvenance unspecified(String fingerprint,ManifestCompleteness completeness){
            return new ManifestProvenance(fingerprint,ManifestOriginStage.UNSPECIFIED,"UNKNOWN",0,0,0,0,
                    ManifestConstructionStatus.UNRESOLVED,ManifestInheritedApiStatus.UNRESOLVED,
                    ManifestCorrelationStatus.INSUFFICIENT,false,0,0,0,false,
                    completeness==ManifestCompleteness.COMPLETE?List.of():List.of(ManifestPartialReason.SOURCE_INDEX_INCOMPLETE));
        }
        public static ManifestProvenance empty(){return unspecified(null,ManifestCompleteness.PARTIAL);}
    }
    public record PageObjectCapability(String classId,String pageId,String qualifiedName,String logicalPath,
                                       String accessibility,List<ConstructorCapability>constructors,
                                       List<MethodCapability>methods){
        public PageObjectCapability{constructors=sorted(constructors,Comparator.comparing(ConstructorCapability::signature));methods=sorted(methods,Comparator.comparing(MethodCapability::signature).thenComparing(MethodCapability::methodId));}
    }
    public record ConstructorCapability(String signature,List<CapabilityParameter>parameters,
                                        String accessibility,String acquisition,boolean implicit){
        public ConstructorCapability{parameters=List.copyOf(parameters==null?List.of():parameters);}
    }
    public record MethodCapability(String methodId,String name,String signature,List<CapabilityParameter>parameters,
                                   String returnType,boolean staticMethod,String declaringType,String classification,
                                   List<String>applicationElementIds){
        public MethodCapability(String methodId,String name,String signature,List<CapabilityParameter>parameters,
                                String returnType,boolean staticMethod,String classification,
                                List<String>applicationElementIds){
            this(methodId,name,signature,parameters,returnType,staticMethod,null,classification,applicationElementIds);
        }
        public MethodCapability{parameters=List.copyOf(parameters==null?List.of():parameters);applicationElementIds=canonical(applicationElementIds);}
    }
    public record CapabilityParameter(String name,String type){}
    public enum ManifestCompleteness{COMPLETE,PARTIAL}
    public enum ManifestOriginStage{SOURCE_CONTEXT_SLICE,RESTORED_SNAPSHOT,UNSPECIFIED}
    public enum ManifestPartialReason{SOURCE_INDEX_INCOMPLETE,REQUIRED_CLASS_MISSING,REQUIRED_API_UNRESOLVED,
        CONSTRUCTION_UNRESOLVED,CORRELATION_INSUFFICIENT,MANIFEST_TRUNCATED,FINGERPRINT_MISMATCH,MANIFEST_STALE}
    public enum ManifestConstructionStatus{VERIFIED,UNRESOLVED,NOT_REQUIRED}
    public enum ManifestInheritedApiStatus{VERIFIED,UNRESOLVED,NOT_APPLICABLE}
    public enum ManifestCorrelationStatus{EXACT,STRONG,MIXED,INSUFFICIENT}
    public enum Completeness{COMPLETE_FOR_REQUESTED_SCOPE,PARTIAL,FAILED}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    private static List<ScopeDecision>decisions(List<ScopeDecision>v){return(v==null?List.<ScopeDecision>of():v).stream().sorted(Comparator.comparing(ScopeDecision::kind).thenComparing(ScopeDecision::id)).toList();}
    private static <T>List<T> sorted(List<T>v,Comparator<? super T>comparator){return(v==null?List.<T>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted(comparator).toList();}
}
