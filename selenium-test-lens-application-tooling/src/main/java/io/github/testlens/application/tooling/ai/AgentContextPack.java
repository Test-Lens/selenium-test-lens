package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded, redacted task context; it deliberately contains projections rather than the complete model. @since 0.5.0 */
public record AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,
                               Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,
                               SourceContext source,List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness) {
    public AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness){this(header,task,pages,pageObjectApis,existingTestConventions,SourceContext.empty(),included,excluded,completeness);}
    public AgentContextPack{if(header==null||task==null||completeness==null)throw new IllegalArgumentException("header, task and completeness are required");pages=(pages==null?List.<PageContext>of():pages).stream().sorted(Comparator.comparing(PageContext::pageId)).toList();var map=new TreeMap<String,List<String>>();if(pageObjectApis!=null)pageObjectApis.forEach((key,value)->map.put(key,canonical(value)));pageObjectApis=Map.copyOf(map);existingTestConventions=canonical(existingTestConventions);source=source==null?SourceContext.empty():source;included=decisions(included);excluded=decisions(excluded);}
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
    public enum Completeness{COMPLETE_FOR_REQUESTED_SCOPE,PARTIAL,FAILED}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    private static List<ScopeDecision>decisions(List<ScopeDecision>v){return(v==null?List.<ScopeDecision>of():v).stream().sorted(Comparator.comparing(ScopeDecision::kind).thenComparing(ScopeDecision::id)).toList();}
    private static <T>List<T> sorted(List<T>v,Comparator<? super T>comparator){return(v==null?List.<T>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted(comparator).toList();}
}
