package io.github.testlens.application.tooling.ai;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.application.tooling.source.PageObjectCorrelation;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.tooling.ExistingProjectIndex;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Creates a small dependency-closed and redacted context from explicit stable IDs. @since 0.5.0 */
public final class ContextSlicer {
    /** Source-aware V2 slice. Only evidence-backed correlations are included; raw source and selector values are excluded. */
    public AgentContextPack slice(ApplicationModel model,AgentTask task,Map<String,List<String>>pageObjectApis,
                                  List<String>existingTestConventions,RedactionPolicy redaction,Limits limits,
                                  ExistingProjectIndex sourceIndex,PageObjectCorrelation correlation,SourceLimits sourceLimits){
        AgentContextPack base=slice(model,task,pageObjectApis,existingTestConventions,redaction,limits);
        Objects.requireNonNull(sourceIndex,"sourceIndex");Objects.requireNonNull(correlation,"correlation");Objects.requireNonNull(sourceLimits,"sourceLimits");
        Set<String>pageIds=base.pages().stream().map(AgentContextPack.PageContext::pageId).collect(java.util.stream.Collectors.toSet());
        Set<String>elementIds=base.pages().stream().flatMap(page->page.elements().stream()).map(AgentContextPack.ElementContext::elementId).collect(java.util.stream.Collectors.toSet());
        Map<String,String>classPages=new HashMap<>();
        correlation.classes().stream().filter(value->pageIds.contains(value.pageId())&&trusted(value.state())).forEach(value->classPages.put(value.classId(),value.pageId()));
        Map<String,PageObjectCorrelation.ElementCorrelation>sourceCorrelations=new HashMap<>();
        correlation.elements().stream().filter(value->elementIds.contains(value.applicationElementId())&&value.sourceElementId()!=null&&trusted(value.state())).forEach(value->sourceCorrelations.put(value.sourceElementId(),value));
        Map<String,ExistingProjectIndex.ClassEntry>classesById=new HashMap<>();sourceIndex.classes().forEach(value->classesById.put(value.id(),value));
        Map<String,ExistingProjectIndex.ElementEntry>elementsById=new HashMap<>();sourceIndex.elements().forEach(value->elementsById.put(value.id(),value));
        List<AgentContextPack.ScopeDecision>included=new ArrayList<>(base.included()),excluded=new ArrayList<>(base.excluded());
        List<AgentContextPack.PageObjectClassContext>classes=new ArrayList<>();int estimated=0;boolean truncated=false;
        for(var entry:sourceIndex.classes())if(classPages.containsKey(entry.id())){
            if(classes.size()>=sourceLimits.maxClasses()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_CLASS","maxClasses reached"));truncated=true;continue;}
            var value=new AgentContextPack.PageObjectClassContext(redaction.redact(entry.id()),redaction.redact(classPages.get(entry.id())),redaction.redact(entry.qualifiedName()),redaction.redact(entry.logicalPath()),entry.classification().name(),entry.origin().name(),List.of("CORRELATED_TO_INCLUDED_PAGE"));
            estimated+=estimate(value);if(estimated>sourceLimits.maxSerializedCharacters()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_CLASS","maxSerializedCharacters reached"));truncated=true;break;}classes.add(value);included.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_CLASS","Evidence-backed correlation to included page"));
        }
        Set<String>classIds=classes.stream().map(AgentContextPack.PageObjectClassContext::classId).collect(java.util.stream.Collectors.toSet());
        List<AgentContextPack.SourceDeclarationContext>declarations=new ArrayList<>();Map<String,String>declarationToApp=new HashMap<>();
        for(var entry:sourceIndex.elements())if(classIds.contains(entry.ownerClassId())&&sourceCorrelations.containsKey(entry.id())){
            if(declarations.size()>=sourceLimits.maxDeclarations()){excluded.add(new AgentContextPack.ScopeDecision(entry.declarationRef(),"SOURCE_DECLARATION","maxDeclarations reached"));truncated=true;continue;}
            var corr=sourceCorrelations.get(entry.id());var value=new AgentContextPack.SourceDeclarationContext(redaction.redact(entry.declarationRef()),redaction.redact(entry.ownerClassId()),redaction.redact(entry.name()),redaction.redact(entry.strategy()),entry.valueProjection()==null?null:redaction.redact(entry.valueProjection().fingerprint()),redaction.redact(corr.applicationElementId()),corr.state().name(),List.of("CORRELATED_APPLICATION_ELEMENT"));
            estimated+=estimate(value);if(estimated>sourceLimits.maxSerializedCharacters()){excluded.add(new AgentContextPack.ScopeDecision(entry.declarationRef(),"SOURCE_DECLARATION","maxSerializedCharacters reached"));truncated=true;break;}declarations.add(value);declarationToApp.put(entry.declarationRef(),corr.applicationElementId());included.add(new AgentContextPack.ScopeDecision(entry.declarationRef(),"SOURCE_DECLARATION","Referenced by correlated Page Object"));
        }
        Set<String>declarationRefs=declarations.stream().map(AgentContextPack.SourceDeclarationContext::declarationRef).collect(java.util.stream.Collectors.toSet());
        Map<String,List<String>>methodDeclarations=new HashMap<>();for(var edge:sourceIndex.edges())if(edge.type()==ExistingProjectIndex.EdgeType.METHOD_TO_DECLARATION&&declarationRefs.contains(edge.toId()))methodDeclarations.computeIfAbsent(edge.fromId(),key->new ArrayList<>()).add(edge.toId());
        Set<String>relevantMethodIds=new HashSet<>(methodDeclarations.keySet());sourceIndex.methods().stream().filter(entry->entry.declarationRefs().stream().anyMatch(declarationRefs::contains)).forEach(entry->relevantMethodIds.add(entry.id()));boolean changed=true;while(changed&&relevantMethodIds.size()<sourceLimits.maxMethods()){changed=false;for(var edge:sourceIndex.edges())if(edge.type()==ExistingProjectIndex.EdgeType.METHOD_TO_METHOD&&relevantMethodIds.contains(edge.toId())&&relevantMethodIds.add(edge.fromId()))changed=true;}
        List<AgentContextPack.PageObjectMethodContext>methods=new ArrayList<>();
        for(var entry:sourceIndex.methods())if(classIds.contains(entry.ownerClassId())&&relevantMethodIds.contains(entry.id())){
            if(methods.size()>=sourceLimits.maxMethods()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_METHOD","maxMethods reached"));truncated=true;continue;}
            List<String>refs=new ArrayList<>(entry.declarationRefs());refs.addAll(methodDeclarations.getOrDefault(entry.id(),List.of()));List<String>appRefs=refs.stream().map(declarationToApp::get).filter(Objects::nonNull).distinct().toList();var value=new AgentContextPack.PageObjectMethodContext(redaction.redact(entry.id()),redaction.redact(entry.ownerClassId()),redaction.redact(entry.signature()),entry.classification().name(),entry.actions().stream().map(redaction::redact).toList(),appRefs.stream().map(redaction::redact).toList(),entry.outgoingTypes().stream().map(redaction::redact).toList(),List.of("USES_CORRELATED_ELEMENT"));
            estimated+=estimate(value);if(estimated>sourceLimits.maxSerializedCharacters()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_METHOD","maxSerializedCharacters reached"));truncated=true;break;}methods.add(value);included.add(new AgentContextPack.ScopeDecision(entry.id(),"PAGE_OBJECT_METHOD","Uses included source declaration"));
        }
        Set<String>methodIds=methods.stream().map(AgentContextPack.PageObjectMethodContext::methodId).collect(java.util.stream.Collectors.toSet());
        Map<String,List<String>>testMethods=new HashMap<>();for(var edge:sourceIndex.edges())if(edge.type()==ExistingProjectIndex.EdgeType.TEST_TO_METHOD&&methodIds.contains(edge.toId()))testMethods.computeIfAbsent(edge.fromId(),key->new ArrayList<>()).add(edge.toId());
        List<AgentContextPack.ExistingTestContext>tests=new ArrayList<>();
        for(var entry:sourceIndex.tests())if(testMethods.containsKey(entry.id())){
            if(tests.size()>=sourceLimits.maxTests()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"EXISTING_TEST","maxTests reached"));truncated=true;continue;}
            var value=new AgentContextPack.ExistingTestContext(redaction.redact(entry.id()),redaction.redact(entry.ownerClassId()),redaction.redact(entry.methodId()),entry.framework().name(),entry.tags().stream().map(redaction::redact).toList(),entry.groups().stream().map(redaction::redact).toList(),testMethods.get(entry.id()).stream().map(redaction::redact).toList(),List.of("CALLS_RELEVANT_PAGE_OBJECT_METHOD"));
            estimated+=estimate(value);if(estimated>sourceLimits.maxSerializedCharacters()){excluded.add(new AgentContextPack.ScopeDecision(entry.id(),"EXISTING_TEST","maxSerializedCharacters reached"));truncated=true;break;}tests.add(value);included.add(new AgentContextPack.ScopeDecision(entry.id(),"EXISTING_TEST","Uses relevant Page Object API"));
        }
        AgentContextPack.SourceContext source=new AgentContextPack.SourceContext(classes,methods,declarations,tests);
        AgentContextPack.Completeness completeness=base.completeness()==AgentContextPack.Completeness.PARTIAL||truncated||sourceIndex.completeness()==ExistingProjectIndex.Completeness.PARTIAL?AgentContextPack.Completeness.PARTIAL:base.completeness();
        ContractHeader header=completeness==AgentContextPack.Completeness.PARTIAL?new ContractHeader(base.header().schemaVersion(),ContractHeader.Status.PARTIAL,base.header().evidence(),merge(base.header().limitations(),"Source context is partial or bounded"),base.header().confidence()):base.header();
        return new AgentContextPack(header,base.task(),base.pages(),base.pageObjectApis(),base.existingTestConventions(),source,included,excluded,completeness);
    }
    public AgentContextPack slice(ApplicationModel model,AgentTask task,Map<String,List<String>>pageObjectApis,
                                  List<String>existingTestConventions,RedactionPolicy redaction,Limits limits){
        Objects.requireNonNull(model,"model");Objects.requireNonNull(task,"task");Objects.requireNonNull(redaction,"redaction");Objects.requireNonNull(limits,"limits");
        pageObjectApis=pageObjectApis==null?Map.of():pageObjectApis;existingTestConventions=existingTestConventions==null?List.of():existingTestConventions;
        Budget budget=new Budget(redaction,limits);
        String safeRequirement=budget.text(task.requirement());
        ContractHeader safeTaskHeader=new ContractHeader(task.header().schemaVersion(),task.header().status(),budget.texts(task.header().evidence()),budget.texts(task.header().limitations()),task.header().confidence());
        AgentTask safeTask=new AgentTask(safeTaskHeader,task.type(),safeRequirement,budget.texts(task.requiredPageIds()),budget.texts(task.requiredStateIds()),budget.texts(task.requiredElementIds()),budget.texts(task.constraints()));
        Map<String,ApplicationModel.PageModel>pages=new LinkedHashMap<>();model.pages().forEach(page->pages.put(page.pageId(),page));
        LinkedHashSet<String>selected=new LinkedHashSet<>();ArrayDeque<QueueEntry>queue=new ArrayDeque<>();
        for(String pageId:task.requiredPageIds())if(pages.containsKey(pageId)){selected.add(pageId);queue.add(new QueueEntry(pageId,0));}
        for(ApplicationModel.PageModel page:model.pages())if(page.elements().stream().anyMatch(element->task.requiredElementIds().contains(element.elementId()))||page.states().stream().anyMatch(state->task.requiredStateIds().contains(state.stateId())))if(selected.add(page.pageId()))queue.add(new QueueEntry(page.pageId(),0));
        List<ApplicationModel.Transition>allTransitions=new ArrayList<>(model.transitions());model.pages().forEach(page->allTransitions.addAll(page.transitions()));
        while(!queue.isEmpty()){
            QueueEntry current=queue.removeFirst();if(current.depth()>=limits.transitionDepth())continue;
            for(ApplicationModel.Transition transition:allTransitions)if(transition.sourcePageId().equals(current.pageId())&&transition.targetPageId()!=null&&pages.containsKey(transition.targetPageId())&&selected.size()<limits.maxPages()&&selected.add(transition.targetPageId()))queue.addLast(new QueueEntry(transition.targetPageId(),current.depth()+1));
        }
        List<AgentContextPack.PageContext>contexts=new ArrayList<>();DecisionCollector decisions=new DecisionCollector(limits.maxScopeDecisions());int elementCount=0,transitionCount=0,stateCount=0,stateElementRefs=0;boolean truncated=false;
        Set<String>knownStates=new HashSet<>(),knownElements=new HashSet<>();model.pages().forEach(page->{page.states().forEach(state->knownStates.add(state.stateId()));page.elements().forEach(element->knownElements.add(element.elementId()));});
        for(String pageId:task.requiredPageIds())if(!pages.containsKey(pageId)){decisions.exclude(decision(budget,pageId,"PAGE","Required stable ID is unknown"));truncated=true;}
        for(String stateId:task.requiredStateIds())if(!knownStates.contains(stateId)){decisions.exclude(decision(budget,stateId,"STATE","Required stable ID is unknown"));truncated=true;}
        for(String elementId:task.requiredElementIds())if(!knownElements.contains(elementId)){decisions.exclude(decision(budget,elementId,"ELEMENT","Required stable ID is unknown"));truncated=true;}
        for(ApplicationModel.PageModel page:model.pages()){
            if(!selected.contains(page.pageId())){decisions.exclude(decision(budget,page.pageId(),"PAGE","Not required by explicit IDs or bounded transition closure"));continue;}
            if(contexts.size()>=limits.maxPages()){decisions.exclude(decision(budget,page.pageId(),"PAGE","maxPages reached"));truncated=true;continue;}
            List<AgentContextPack.StateContext>states=new ArrayList<>();for(var state:page.states()){if(stateCount>=limits.maxStates()){decisions.exclude(decision(budget,state.stateId(),"STATE","maxStates reached"));truncated=true;continue;}List<String>refs=new ArrayList<>();for(String ref:state.elementIds()){if(stateElementRefs>=limits.maxStateElementRefs()){decisions.exclude(decision(budget,state.stateId(),"STATE_ELEMENT_REFERENCE","maxStateElementRefs reached"));truncated=true;break;}refs.add(budget.text(ref));stateElementRefs++;}states.add(new AgentContextPack.StateContext(budget.text(state.stateId()),budget.text(state.semanticName()),refs));stateCount++;}
            List<AgentContextPack.ElementContext>elements=new ArrayList<>();for(var element:page.elements()){if(elementCount>=limits.maxElements()){decisions.exclude(decision(budget,element.elementId(),"ELEMENT","maxElements reached"));truncated=true;continue;}elements.add(new AgentContextPack.ElementContext(budget.text(element.elementId()),budget.text(element.semanticName()),budget.text(element.type().name()),budget.texts(element.actions().stream().map(Enum::name).toList()),budget.text(element.selectorQuality().name())));decisions.include(decision(budget,element.elementId(),"ELEMENT","Member of included page"));elementCount++;}
            List<AgentContextPack.TransitionContext>transitions=new ArrayList<>();Set<String>seenTransitions=new HashSet<>();for(var transition:allTransitions.stream().filter(value->value.sourcePageId().equals(page.pageId())).sorted(java.util.Comparator.comparing(ApplicationModel.Transition::transitionId)).toList()){if(!seenTransitions.add(transition.transitionId()))continue;if(transitionCount>=limits.maxTransitions()){decisions.exclude(decision(budget,transition.transitionId(),"TRANSITION","maxTransitions reached"));truncated=true;continue;}transitions.add(transition(budget,transition));transitionCount++;}
            contexts.add(new AgentContextPack.PageContext(budget.text(page.pageId()),budget.text(page.canonicalName()),budget.url(page.identity().normalizedUrlPattern()),states,elements,transitions));decisions.include(decision(budget,page.pageId(),"PAGE","Explicit requirement or transition dependency"));
        }
        Map<String,List<String>>safeApis=new LinkedHashMap<>();int apiCount=0;for(var entry:pageObjectApis.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())if(selected.contains(entry.getKey())){List<String>apis=new ArrayList<>();for(String api:(entry.getValue()==null?List.<String>of():entry.getValue()).stream().filter(Objects::nonNull).distinct().sorted().toList()){if(apiCount>=limits.maxPageObjectApis()){decisions.exclude(decision(budget,entry.getKey(),"PAGE_OBJECT_API","maxPageObjectApis reached"));truncated=true;break;}apis.add(budget.text(api));apiCount++;}safeApis.put(budget.text(entry.getKey()),List.copyOf(apis));}else decisions.exclude(decision(budget,entry.getKey(),"PAGE_OBJECT_API","Page excluded from task slice"));
        List<String>conventions=new ArrayList<>();for(String convention:existingTestConventions.stream().filter(Objects::nonNull).distinct().sorted().toList()){if(conventions.size()>=limits.maxConventions()){decisions.exclude(decision(budget,"conventions","CONVENTION","maxConventions reached"));truncated=true;break;}conventions.add(budget.text(convention));}
        if(budget.truncated()){decisions.exclude(new AgentContextPack.ScopeDecision("context","CONTEXT_BUDGET","maxStringCharacters or maxTotalCharacters reached"));truncated=true;}
        decisions.finish();truncated|=decisions.truncated();
        AgentContextPack.Completeness completeness=truncated?AgentContextPack.Completeness.PARTIAL:AgentContextPack.Completeness.COMPLETE_FOR_REQUESTED_SCOPE;
        ContractHeader header=new ContractHeader(ContractHeader.SCHEMA_VERSION,truncated?ContractHeader.Status.PARTIAL:ContractHeader.Status.READY,List.of("Explicit stable-ID selection","Bounded transition dependency closure"),truncated?List.of("Context limits truncated the requested graph"):List.of(),ContractHeader.Confidence.OBSERVED);
        return new AgentContextPack(header,safeTask,contexts,safeApis,conventions,decisions.included(),decisions.excluded(),completeness);
    }
    private static AgentContextPack.TransitionContext transition(Budget budget,ApplicationModel.Transition t){return new AgentContextPack.TransitionContext(budget.text(t.transitionId()),budget.text(t.action().name()),budget.text(t.elementId()),budget.text(t.targetPageId()),budget.text(t.targetStateId()),budget.text(t.confidence().name()));}
    private static AgentContextPack.ScopeDecision decision(Budget budget,String id,String kind,String reason){String safeId=budget.text(id);return safeId==null||safeId.isBlank()?null:new AgentContextPack.ScopeDecision(safeId,kind,reason);}
    private record QueueEntry(String pageId,int depth){}
    public record SourceLimits(int maxClasses,int maxMethods,int maxDeclarations,int maxTests,int maxSerializedCharacters){public SourceLimits{if(maxClasses<1||maxClasses>5_000||maxMethods<1||maxMethods>50_000||maxDeclarations<1||maxDeclarations>50_000||maxTests<0||maxTests>50_000||maxSerializedCharacters<512||maxSerializedCharacters>16_777_216)throw new IllegalArgumentException("Source context limits are outside supported bounds");}public static SourceLimits defaults(){return new SourceLimits(24,160,160,24,256_000);}}
    private static boolean trusted(PageObjectCorrelation.State state){return state==PageObjectCorrelation.State.EXACT||state==PageObjectCorrelation.State.STRONG;}
    private static int estimate(Object value){return String.valueOf(value).length();}
    private static List<String>merge(List<String>values,String value){var result=new ArrayList<>(values);result.add(value);return result;}
    public record Limits(int maxPages,int maxElements,int maxTransitions,int transitionDepth,int maxStates,int maxStateElementRefs,int maxPageObjectApis,int maxConventions,int maxStringCharacters,int maxTotalCharacters,int maxScopeDecisions){public Limits(int maxPages,int maxElements,int maxTransitions,int transitionDepth){this(maxPages,maxElements,maxTransitions,transitionDepth,100,1_000,200,50,4_096,256_000,1_000);}public Limits(int maxPages,int maxElements,int maxTransitions,int transitionDepth,int maxStates,int maxStateElementRefs,int maxPageObjectApis,int maxConventions,int maxStringCharacters,int maxTotalCharacters){this(maxPages,maxElements,maxTransitions,transitionDepth,maxStates,maxStateElementRefs,maxPageObjectApis,maxConventions,maxStringCharacters,maxTotalCharacters,1_000);}public Limits{if(maxPages<1||maxPages>500||maxElements<1||maxElements>10_000||maxTransitions<0||maxTransitions>10_000||transitionDepth<0||transitionDepth>10||maxStates<0||maxStates>10_000||maxStateElementRefs<0||maxStateElementRefs>100_000||maxPageObjectApis<0||maxPageObjectApis>10_000||maxConventions<0||maxConventions>10_000||maxStringCharacters<16||maxStringCharacters>1_048_576||maxTotalCharacters<256||maxTotalCharacters>16_777_216||maxScopeDecisions<1||maxScopeDecisions>100_000)throw new IllegalArgumentException("Context limits are outside supported bounds");}public static Limits defaults(){return new Limits(12,200,100,2);}}
    private static final class DecisionCollector{private final int max;private final List<AgentContextPack.ScopeDecision>included=new ArrayList<>(),excluded=new ArrayList<>();private int omitted;private boolean finished;private DecisionCollector(int max){this.max=max;}private void include(AgentContextPack.ScopeDecision value){add(included,value);}private void exclude(AgentContextPack.ScopeDecision value){add(excluded,value);}private void add(List<AgentContextPack.ScopeDecision>target,AgentContextPack.ScopeDecision value){if(value!=null&&size()<max-1)target.add(value);else omitted++;}private int size(){return included.size()+excluded.size();}private void finish(){if(finished)return;finished=true;if(omitted>0)excluded.add(new AgentContextPack.ScopeDecision("scope-decisions","SCOPE_DECISION_LIMIT","maxScopeDecisions reached; "+omitted+" additional decisions omitted"));}private boolean truncated(){return omitted>0;}private List<AgentContextPack.ScopeDecision>included(){return included;}private List<AgentContextPack.ScopeDecision>excluded(){return excluded;}}
    private static final class Budget{private final RedactionPolicy redaction;private final Limits limits;private int characters;private boolean truncated;private Budget(RedactionPolicy redaction,Limits limits){this.redaction=redaction;this.limits=limits;}private String text(String value){return value==null?null:bounded(redaction.redact(value));}private String url(String value){return value==null?null:bounded(redaction.redactUrl(value));}private List<String>texts(List<String>values){return(values==null?List.<String>of():values).stream().filter(Objects::nonNull).map(this::text).toList();}private String bounded(String value){String result=value;if(result.length()>limits.maxStringCharacters()){result=result.substring(0,limits.maxStringCharacters());truncated=true;}int remaining=limits.maxTotalCharacters()-characters;if(remaining<=0){truncated=true;return "";}if(result.length()>remaining){result=result.substring(0,remaining);truncated=true;}characters+=result.length();return result;}private boolean truncated(){return truncated;}}
}
