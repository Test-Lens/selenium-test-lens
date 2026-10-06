package io.github.testlens.studio;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.ai.ContextSlicer;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.selector.tooling.ExistingProjectIndex;

import java.util.*;

/** Deterministic requirement/source-usage scope seed for the domain ContextSlicer. */
final class RequirementContextSelector {
    record Selection(List<String> pageIds,List<String> stateIds,List<String> elementIds){}
    Selection select(String requirement,ApplicationModel application,ExistingProjectIndex source,PageObjectCorrelation correlation){
        Set<String> tokens=Arrays.stream(requirement.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(value->value.length()>=3).filter(value->!Set.of("the","and","for","with","should","when","then","display","show").contains(value)).collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        if(tokens.isEmpty())throw new IllegalArgumentException("Requirement does not contain a specific application or source term");
        Map<String,Integer> scores=new HashMap<>();Map<String,ApplicationModel.PageModel> pages=new HashMap<>();
        for(var page:application.pages()){
            pages.put(page.pageId(),page);int score=matches(tokens,page.canonicalName())*8;
            for(var element:page.elements())score+=matches(tokens,element.semanticName())*5+matches(tokens,element.label())*4+matches(tokens,element.accessibleName())*4+matches(tokens,element.semanticRole())*2;
            for(var state:page.states())score+=matches(tokens,state.semanticName())*4;
            if(score>0)scores.merge(page.pageId(),score,Integer::sum);
        }
        Map<String,String> classPages=new HashMap<>();correlation.classes().stream().filter(value->value.state()==PageObjectCorrelation.State.EXACT||value.state()==PageObjectCorrelation.State.STRONG).forEach(value->classPages.put(value.classId(),value.pageId()));
        Map<String,ExistingProjectIndex.ClassEntry> classes=new HashMap<>();source.classes().forEach(value->classes.put(value.id(),value));
        Map<String,ExistingProjectIndex.MethodEntry> methods=new HashMap<>();source.methods().forEach(value->methods.put(value.id(),value));
        for(var method:source.methods())if(classPages.containsKey(method.ownerClassId())){
            int score=matches(tokens,method.name())*7+matches(tokens,method.signature())*5+method.actions().stream().mapToInt(value->matches(tokens,value)*2).sum();
            ExistingProjectIndex.ClassEntry owner=classes.get(method.ownerClassId());if(owner!=null)score+=matches(tokens,owner.simpleName())*6;
            if(score>0)scores.merge(classPages.get(method.ownerClassId()),score,Integer::sum);
        }
        for(var origin:source.methods()){
            int relevance=matches(tokens,origin.name())*7+matches(tokens,origin.signature())*5;if(relevance==0)continue;
            source.edges().stream().filter(edge->(edge.type()==ExistingProjectIndex.EdgeType.METHOD_TO_METHOD||edge.type()==ExistingProjectIndex.EdgeType.TEST_TO_METHOD)&&edge.fromId().equals(origin.id()))
                    .map(edge->methods.get(edge.toId())).filter(Objects::nonNull).map(method->classPages.get(method.ownerClassId())).filter(Objects::nonNull)
                    .forEach(pageId->scores.merge(pageId,relevance,Integer::sum));
        }
        for(var test:source.tests()){
            ExistingProjectIndex.MethodEntry testMethod=methods.get(test.methodId());int relevance=testMethod==null?0:matches(tokens,testMethod.name())*7+matches(tokens,testMethod.signature())*5;if(relevance==0)continue;
            source.edges().stream().filter(edge->edge.type()==ExistingProjectIndex.EdgeType.TEST_TO_METHOD&&edge.fromId().equals(test.id()))
                    .map(edge->methods.get(edge.toId())).filter(Objects::nonNull).map(method->classPages.get(method.ownerClassId())).filter(Objects::nonNull)
                    .forEach(pageId->scores.merge(pageId,relevance,Integer::sum));
        }
        List<String> pageIds=scores.entrySet().stream().filter(value->pages.containsKey(value.getKey()))
                .sorted(Map.Entry.<String,Integer>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .limit(ContextSlicer.Limits.defaults().maxPages()).map(Map.Entry::getKey).toList();
        if(pageIds.isEmpty())throw new IllegalArgumentException("Requirement could not be related to mapped application or indexed source usage");
        Set<String> selectedPages=Set.copyOf(pageIds);
        List<String> elementIds=application.pages().stream().filter(value->selectedPages.contains(value.pageId())).flatMap(value->value.elements().stream())
                .filter(value->matches(tokens,value.semanticName())+matches(tokens,value.label())+matches(tokens,value.accessibleName())>0).map(ApplicationModel.ElementModel::elementId).sorted().toList();
        List<String> stateIds=application.pages().stream().filter(value->selectedPages.contains(value.pageId())).flatMap(value->value.states().stream())
                .filter(value->matches(tokens,value.semanticName())>0).map(ApplicationModel.PageState::stateId).sorted().toList();
        return new Selection(pageIds,stateIds,elementIds);
    }
    private static int matches(Set<String> tokens,String value){if(value==null)return 0;String normalized=value.replaceAll("([a-z])([A-Z])","$1 $2").toLowerCase(Locale.ROOT);return(int)tokens.stream().filter(normalized::contains).count();}
}
