package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded, redacted task context; it deliberately contains projections rather than the complete model. @since 0.5.0 */
public record AgentContextPack(ContractHeader header,AgentTask task,List<PageContext>pages,
                               Map<String,List<String>>pageObjectApis,List<String>existingTestConventions,
                               List<ScopeDecision>included,List<ScopeDecision>excluded,Completeness completeness) {
    public AgentContextPack{if(header==null||task==null||completeness==null)throw new IllegalArgumentException("header, task and completeness are required");pages=(pages==null?List.<PageContext>of():pages).stream().sorted(Comparator.comparing(PageContext::pageId)).toList();var map=new TreeMap<String,List<String>>();if(pageObjectApis!=null)pageObjectApis.forEach((key,value)->map.put(key,canonical(value)));pageObjectApis=Map.copyOf(map);existingTestConventions=canonical(existingTestConventions);included=decisions(included);excluded=decisions(excluded);}
    public record PageContext(String pageId,String canonicalName,String urlPattern,List<StateContext>states,List<ElementContext>elements,List<TransitionContext>transitions){public PageContext{states=(states==null?List.<StateContext>of():states).stream().sorted(Comparator.comparing(StateContext::stateId)).toList();elements=(elements==null?List.<ElementContext>of():elements).stream().sorted(Comparator.comparing(ElementContext::elementId)).toList();transitions=(transitions==null?List.<TransitionContext>of():transitions).stream().sorted(Comparator.comparing(TransitionContext::transitionId)).toList();}}
    public record StateContext(String stateId,String semanticName,List<String>elementIds){public StateContext{elementIds=canonical(elementIds);}}
    public record ElementContext(String elementId,String semanticName,String type,List<String>actions,String selectorQuality){public ElementContext{actions=canonical(actions);}}
    public record TransitionContext(String transitionId,String action,String elementId,String targetPageId,String targetStateId,String confidence){}
    public record ScopeDecision(String id,String kind,String reason){}
    public enum Completeness{COMPLETE_FOR_REQUESTED_SCOPE,PARTIAL,FAILED}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    private static List<ScopeDecision>decisions(List<ScopeDecision>v){return(v==null?List.<ScopeDecision>of():v).stream().sorted(Comparator.comparing(ScopeDecision::kind).thenComparing(ScopeDecision::id)).toList();}
}
