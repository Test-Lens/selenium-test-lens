package io.github.testlens.application.tooling.drift;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.store.ApplicationModelFingerprint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** ID-based semantic differ. Renames are reported only when the stable element ID is unchanged. @since 0.5.0 */
public final class ApplicationModelDiffer {
    public ApplicationDrift compare(ApplicationModel before, ApplicationModel after) {
        Objects.requireNonNull(before,"before"); Objects.requireNonNull(after,"after");
        if (!before.applicationId().equals(after.applicationId())) throw new IllegalArgumentException("Models belong to different applications");
        List<ApplicationDrift.Change> changes = new ArrayList<>();
        Map<String,ApplicationModel.PageModel> oldPages=index(before.pages(),ApplicationModel.PageModel::pageId);
        Map<String,ApplicationModel.PageModel> newPages=index(after.pages(),ApplicationModel.PageModel::pageId);
        removedAdded(changes,oldPages,newPages,ApplicationDrift.ChangeType.PAGE_REMOVED,ApplicationDrift.ChangeType.PAGE_ADDED,ApplicationDrift.EntityType.PAGE,null);
        for(String pageId:intersection(oldPages,newPages))comparePage(changes,oldPages.get(pageId),newPages.get(pageId));
        compareTransitions(changes,before.transitions(),after.transitions(),null);
        return new ApplicationDrift(ApplicationDrift.SCHEMA_VERSION,before.applicationId(),ApplicationModelFingerprint.semantic(before),ApplicationModelFingerprint.semantic(after),changes,List.of("Renames are not inferred across different stable IDs"));
    }

    private static void comparePage(List<ApplicationDrift.Change> changes,ApplicationModel.PageModel before,ApplicationModel.PageModel after){
        Map<String,ApplicationModel.PageState> oldStates=index(before.states(),ApplicationModel.PageState::stateId),newStates=index(after.states(),ApplicationModel.PageState::stateId);
        removedAdded(changes,oldStates,newStates,ApplicationDrift.ChangeType.STATE_REMOVED,ApplicationDrift.ChangeType.STATE_ADDED,ApplicationDrift.EntityType.STATE,before.pageId());
        Map<String,ApplicationModel.ElementModel> oldElements=index(before.elements(),ApplicationModel.ElementModel::elementId),newElements=index(after.elements(),ApplicationModel.ElementModel::elementId);
        removedAdded(changes,oldElements,newElements,ApplicationDrift.ChangeType.ELEMENT_REMOVED,ApplicationDrift.ChangeType.ELEMENT_ADDED,ApplicationDrift.EntityType.ELEMENT,before.pageId());
        for(String id:intersection(oldElements,newElements)){
            ApplicationModel.ElementModel oldElement=oldElements.get(id),newElement=newElements.get(id);
            ApplicationDrift.SourceCorrelation correlation=correlation(oldElement,newElement);
            if(!oldElement.semanticName().equals(newElement.semanticName()))changes.add(change(ApplicationDrift.ChangeType.ELEMENT_RENAMED,ApplicationDrift.EntityType.ELEMENT,id,before.pageId(),oldElement.semanticName(),newElement.semanticName(),"Stable element identity retained",correlation));
            String oldSelector=selector(oldElement.preferredSelector()),newSelector=selector(newElement.preferredSelector());
            List<String>selectorEvidence=selectorEvidence(oldElement.preferredSelector(),newElement.preferredSelector());
            if(!Objects.equals(oldSelector,newSelector))changes.add(change(ApplicationDrift.ChangeType.SELECTOR_CHANGED,ApplicationDrift.EntityType.ELEMENT,id,before.pageId(),oldSelector,newSelector,"Preferred selector or its structured validation evidence changed",correlation,selectorEvidence));
            if(oldElement.selectorQuality()!=newElement.selectorQuality()){
                ApplicationDrift.ChangeType type=qualityRank(newElement.selectorQuality())>qualityRank(oldElement.selectorQuality())?ApplicationDrift.ChangeType.SELECTOR_IMPROVED:ApplicationDrift.ChangeType.SELECTOR_BECAME_UNSTABLE;
                changes.add(change(type,ApplicationDrift.EntityType.ELEMENT,id,before.pageId(),oldElement.selectorQuality().name(),newElement.selectorQuality().name(),"Selector quality changed",correlation,selectorEvidence));
            }else if(oldElement.preferredSelector()!=null&&newElement.preferredSelector()!=null){
                boolean degraded=degraded(oldElement.preferredSelector(),newElement.preferredSelector()),improved=degraded(newElement.preferredSelector(),oldElement.preferredSelector());
                if(degraded&&!improved)changes.add(change(ApplicationDrift.ChangeType.SELECTOR_BECAME_UNSTABLE,ApplicationDrift.EntityType.ELEMENT,id,before.pageId(),oldSelector,newSelector,"Structured selector evidence degraded",correlation,selectorEvidence));
                else if(improved&&!degraded)changes.add(change(ApplicationDrift.ChangeType.SELECTOR_IMPROVED,ApplicationDrift.EntityType.ELEMENT,id,before.pageId(),oldSelector,newSelector,"Structured selector evidence improved",correlation,selectorEvidence));
            }
        }
        compareTransitions(changes,before.transitions(),after.transitions(),before.pageId());
    }
    private static void compareTransitions(List<ApplicationDrift.Change>changes,List<ApplicationModel.Transition>oldValues,List<ApplicationModel.Transition>newValues,String parent){
        Map<String,ApplicationModel.Transition>oldMap=index(oldValues,ApplicationModel.Transition::transitionId),newMap=index(newValues,ApplicationModel.Transition::transitionId);
        removedAdded(changes,oldMap,newMap,ApplicationDrift.ChangeType.TRANSITION_REMOVED,ApplicationDrift.ChangeType.TRANSITION_ADDED,ApplicationDrift.EntityType.TRANSITION,parent);
        for(String id:intersection(oldMap,newMap)){String oldValue=transition(oldMap.get(id)),newValue=transition(newMap.get(id));if(!oldValue.equals(newValue))changes.add(change(ApplicationDrift.ChangeType.TRANSITION_CHANGED,ApplicationDrift.EntityType.TRANSITION,id,parent,oldValue,newValue,"Observed transition contract changed",ApplicationDrift.SourceCorrelation.NO_SOURCE_CORRELATION));}
    }
    private static <T>void removedAdded(List<ApplicationDrift.Change>changes,Map<String,T>oldMap,Map<String,T>newMap,ApplicationDrift.ChangeType removed,ApplicationDrift.ChangeType added,ApplicationDrift.EntityType entity,String parent){
        oldMap.keySet().stream().filter(id->!newMap.containsKey(id)).forEach(id->changes.add(change(removed,entity,id,parent,"present",null,"Stable ID absent from new model",ApplicationDrift.SourceCorrelation.NO_SOURCE_CORRELATION)));
        newMap.keySet().stream().filter(id->!oldMap.containsKey(id)).forEach(id->changes.add(change(added,entity,id,parent,null,"present","Stable ID absent from previous model",ApplicationDrift.SourceCorrelation.NO_SOURCE_CORRELATION)));
    }
    private static ApplicationDrift.Change change(ApplicationDrift.ChangeType type,ApplicationDrift.EntityType entity,String id,String parent,String before,String after,String reason,ApplicationDrift.SourceCorrelation correlation){return new ApplicationDrift.Change(type,entity,id,parent,before,after,reason,correlation,List.of());}
    private static ApplicationDrift.Change change(ApplicationDrift.ChangeType type,ApplicationDrift.EntityType entity,String id,String parent,String before,String after,String reason,ApplicationDrift.SourceCorrelation correlation,List<String>evidence){return new ApplicationDrift.Change(type,entity,id,parent,before,after,reason,correlation,evidence);}
    private static ApplicationDrift.SourceCorrelation correlation(ApplicationModel.ElementModel before,ApplicationModel.ElementModel after){return sourceRef(before)!=null&&sourceRef(before).equals(sourceRef(after))?ApplicationDrift.SourceCorrelation.CORRELATED:ApplicationDrift.SourceCorrelation.NO_SOURCE_CORRELATION;}
    private static String sourceRef(ApplicationModel.ElementModel element){return element.provenance().attributes().get("sourceDeclaration");}
    private static String selector(ApplicationModel.SelectorProjection value){return value==null?null:String.join("|",value.strategy(),value.value(),value.candidateId(),Objects.toString(value.validation(),""),Objects.toString(value.sameTarget(),""),Boolean.toString(value.unique()),String.join(",",value.stability()),String.join(",",value.reasons()),String.join(",",value.limitations()));}
    private static List<String>selectorEvidence(ApplicationModel.SelectorProjection before,ApplicationModel.SelectorProjection after){List<String>evidence=new ArrayList<>();evidence.add("before="+selector(before));evidence.add("after="+selector(after));return evidence;}
    private static boolean degraded(ApplicationModel.SelectorProjection before,ApplicationModel.SelectorProjection after){return(before.unique()&&!after.unique())||(sameTarget(before)&&!sameTarget(after))||(validated(before)&&!validated(after))||(stable(before)&&!stable(after));}
    private static boolean sameTarget(ApplicationModel.SelectorProjection selector){String value=Objects.toString(selector.sameTarget(),"").toUpperCase(java.util.Locale.ROOT);return value.equals("SAME_TARGET")||value.equals("TRUE")||value.equals("MATCH");}
    private static boolean validated(ApplicationModel.SelectorProjection selector){String value=Objects.toString(selector.validation(),"").toUpperCase(java.util.Locale.ROOT);return value.contains("VERIFIED")||value.equals("VALID")||value.equals("SUCCESS");}
    private static boolean stable(ApplicationModel.SelectorProjection selector){return selector.stability().stream().map(value->value.toUpperCase(java.util.Locale.ROOT)).anyMatch(value->value.equals("STABLE")||value.endsWith("_STABLE"));}
    private static String transition(ApplicationModel.Transition value){return String.join("|",value.sourcePageId(),Objects.toString(value.sourceStateId(),""),Objects.toString(value.elementId(),""),value.action().name(),Objects.toString(value.targetPageId(),""),Objects.toString(value.targetStateId(),""),value.confidence().name());}
    private static int qualityRank(ApplicationModel.SelectorQuality quality){return switch(quality){case UNAVAILABLE->0;case REVIEW_REQUIRED->1;case VERIFIED->2;};}
    private static <T>Map<String,T>index(List<T>values,Function<T,String>id){return values.stream().collect(Collectors.toMap(id,Function.identity(),(left,right)->right,LinkedHashMap::new));}
    private static <T>List<String>intersection(Map<String,T>left,Map<String,T>right){return left.keySet().stream().filter(right::containsKey).sorted().toList();}
}
