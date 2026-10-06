package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationIds;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.live.LiveCandidateAnalysisService;
import io.github.testlens.selector.live.LiveCandidateRequest;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import java.net.URI;
import java.util.*;

/**
 * Explicit synchronous mapper for the caller-owned current browsing context.
 * Construction and ordinary Test Lens runtime perform no mapping commands; only {@link #observe()} touches WebDriver.
 * @since 0.5.0
 */
public final class ApplicationMapper {
    private final WebDriver driver;private final ApplicationMapperOptions options;private final String applicationId;
    private final PageScanner scanner=new PageScanner();private final LiveCandidateAnalysisService selectorService=new LiveCandidateAnalysisService();
    private final SelectorProjectionAdapter projections=new SelectorProjectionAdapter();private final ApplicationAggregator aggregator;private final PageIdentityResolver identityResolver;
    private MappingMetrics metrics=new MappingMetrics(0,0,0,0,0,0,0,0,0,0,0,0,0);
    private String currentPageId,currentStateId;private Map<String,PageScanner.DiscoveredElement>lastTargets=Map.of();private Map<String,ApplicationModel.ElementModel>lastElements=Map.of();
    private PendingTransition pending;

    private ApplicationMapper(WebDriver driver,ApplicationMapperOptions options){this.driver=Objects.requireNonNull(driver,"driver");this.options=Objects.requireNonNull(options,"options");
        applicationId=ApplicationIds.id("application-v1",options.applicationName());aggregator=new ApplicationAggregator(applicationId,options.applicationName(),options);identityResolver=new PageIdentityResolver(applicationId,options.overrides());}
    public static ApplicationMapper start(WebDriver driver,ApplicationMapperOptions options){return new ApplicationMapper(driver,options);}

    public MappingObservation observe(){String rawUrl=safe(driver::getCurrentUrl,"current URL"),title=redact(safe(driver::getTitle,"title"));String pattern=UrlPatternNormalizer.normalize(rawUrl,options.redactionPolicy());String context=context(pattern);
        PageScanner.Snapshot snapshot=scanner.scan(driver,options);List<ApplicationModel.Limitation>limitations=new ArrayList<>();boolean truncated=snapshot.truncated();
        if(snapshot.truncated())limitations.add(new ApplicationModel.Limitation("MAX_DISCOVERED_NODES","Meaningful discovery reached "+options.maxDiscoveredNodes()));
        if(snapshot.closedShadowPossible()>0)limitations.add(new ApplicationModel.Limitation("CLOSED_SHADOW_POSSIBLE",snapshot.closedShadowPossible()+" custom elements had no inspectable open root"));
        if(snapshot.shadowDepthReached()>=options.maxShadowDepth()&&snapshot.shadowRoots()>0)limitations.add(new ApplicationModel.Limitation("MAX_SHADOW_DEPTH","Open shadow traversal reached depth "+options.maxShadowDepth()));
        List<PageScanner.DiscoveredElement>meaningful=snapshot.elements().stream().limit(options.maxActionableElements()).toList();
        if(snapshot.elements().size()>meaningful.size()){truncated=true;limitations.add(new ApplicationModel.Limitation("MAX_ACTIONABLE_ELEMENTS","Actionable analysis was bounded at "+options.maxActionableElements()));}
        PageIdentityResolver.Resolution pageResolution=identityResolver.resolve(pattern,title,meaningful,options.preferredTestAttributes(),options.redactionPolicy());String pageId=pageResolution.pageId();String configuredPageName=pageResolution.identityGroup()==null?null:options.overrides().pageNamesByIdentityGroup().get(pageResolution.identityGroup());String pageName=configuredPageName==null?SemanticNaming.pageName(pattern,title,options.overrides()):pageName(configuredPageName);
        Map<String,ApplicationModel.Region>regions=regions(snapshot,pageId,context);List<ElementDraft>drafts=new ArrayList<>();Map<String,PageScanner.DiscoveredElement>targets=new LinkedHashMap<>();
        Map<String,Integer>identityCounts=new HashMap<>();for(PageScanner.DiscoveredElement element:meaningful){String regionFingerprint=regionFingerprint(regions,element.regionKey());String base=elementIdentityBase(element,regionFingerprint);identityCounts.merge(base,1,Integer::sum);}Map<String,Integer>ambiguousOccurrences=new HashMap<>();
        long selectorStart=System.nanoTime();int selectorAnalyses=0,seleniumCommands=0,index=0;
        for(PageScanner.DiscoveredElement element:meaningful){if(selectorAnalyses>=options.maxCandidateAnalyses()){truncated=true;limitations.add(new ApplicationModel.Limitation("MAX_CANDIDATE_ANALYSES","Selector analysis was bounded at "+options.maxCandidateAnalyses()));break;}
            CandidateAnalysis analysis;
            try{analysis=selectorService.analyze(new LiveCandidateRequest(driver,element.context(),element.target(),CandidateAnalysis.UsageIntent.FIND_ONE,null,context,element.shadow(),null,null,null,null,null,null,options.selectorPolicies(),options.selectorEvidence(),options.preferredTestAttributes()));}
            catch(StaleElementReferenceException stale){limitations.add(new ApplicationModel.Limitation("TARGET_STALE","A discovered target became stale before analysis"));continue;}
            selectorAnalyses++;seleniumCommands+=analysis.completeness().validationCommands();SelectorProjectionAdapter.Projection selector=projections.project(analysis,options.redactionPolicy());
            for(CandidateAnalysis.Issue issue:analysis.issues())limitations.add(new ApplicationModel.Limitation(issue.code(),first(redact(issue.detail()),"Selector analysis issue")));
            String label=redact(element.label()),accessible=redact(element.accessibleName()),role=redact(element.role()),regionId=regionId(regions,element.regionKey());
            ApplicationModel.ElementType type=SemanticNaming.type(element);String identityBase=elementIdentityBase(element,regionFingerprint(regions,element.regionKey()));boolean ambiguous=identityCounts.getOrDefault(identityBase,0)>1;
            String structural=first(element.structuralHint(),"structural-unavailable");String occurrenceKey=identityBase+'|'+structural;int occurrence=ambiguousOccurrences.merge(occurrenceKey,1,Integer::sum);
            String fingerprint=ambiguous?ApplicationIds.id("element-fingerprint-v1",identityBase,structural,occurrence==1?"":Integer.toString(occurrence)):identityBase;
            String elementId=ApplicationIds.id("element-v1",pageId,fingerprint);String name=SemanticNaming.elementName(element,analysis.candidates(),fingerprint,options.overrides(),options.redactionPolicy(),options.preferredTestAttributes());
            List<ApplicationModel.Limitation>elementLimits=new ArrayList<>();if(selector.quality()!=ApplicationModel.SelectorQuality.VERIFIED)elementLimits.add(new ApplicationModel.Limitation("SELECTOR_"+selector.quality().name(),"Review selector evidence before generated use"));if(ambiguous)elementLimits.add(new ApplicationModel.Limitation("ELEMENT_IDENTITY_AMBIGUOUS","Similar elements required structural positional evidence; review identity after structural changes"));if(occurrence>1)elementLimits.add(new ApplicationModel.Limitation("ELEMENT_IDENTITY_OCCURRENCE_FALLBACK","Structural evidence collided; bounded discovery occurrence was used only to avoid an unsafe merge"));
            ApplicationModel.Provenance provenance=new ApplicationModel.Provenance(ApplicationModel.EvidenceSource.OBSERVED,observationId(pageId,snapshot,index),context,List.of("MEANINGFUL_DISCOVERY","LIVE_CANDIDATE_ANALYSIS"),Map.of("nameSource",nameSource(element)));
            ApplicationModel.ElementModel model=new ApplicationModel.ElementModel(elementId,name,type,role,label,accessible,SemanticNaming.actions(type,element.inputType(),element.disabled()),selector.preferred(),selector.alternatives(),selector.quality(),regionId,fingerprint,provenance,elementLimits);
            if(!targets.containsKey(elementId)){drafts.add(new ElementDraft(model,element));targets.put(elementId,element);}index++;
        }
        long selectorNanos=System.nanoTime()-selectorStart;List<ApplicationModel.ElementModel>elements=resolveNames(drafts);
        String structural=ApplicationIds.id("page-structure-v1",elements.stream().map(e->e.type()+"|"+e.semanticRole()+"|"+e.semanticName()+"|"+(e.regionId()==null?"":e.regionId())).sorted().toArray(String[]::new));
        List<String>landmarks=elements.stream().filter(e->e.selectorQuality()==ApplicationModel.SelectorQuality.VERIFIED).map(ApplicationModel.ElementModel::elementId).limit(8).toList();
        ApplicationModel.EvidenceSource identitySource=pageResolution.source()==ApplicationModel.EvidenceSource.USER_DECLARED||options.overrides().pageNamesByUrlPattern().containsKey(pattern)?ApplicationModel.EvidenceSource.USER_DECLARED:ApplicationModel.EvidenceSource.OBSERVED;
        ApplicationModel.PageIdentity identity=new ApplicationModel.PageIdentity(pattern,path(pattern),title,landmarks,structural,identitySource);
        String stateId=ApplicationIds.id("state-v1",pageId,structural);String stateName=stateName(elements,structural);String observationId=ApplicationIds.id("observation-v1",pageId,stateId,Integer.toString(metrics.observations()+1));
        List<String>pageEvidence=new ArrayList<>(pageResolution.evidence());pageEvidence.add("STRUCTURAL_FINGERPRINT");ApplicationModel.Provenance pageProvenance=new ApplicationModel.Provenance(identitySource,observationId,context,pageEvidence,Map.of("pageObservationFingerprint",pageResolution.observationFingerprint()));
        ApplicationModel.PageState state=new ApplicationModel.PageState(stateId,stateName,List.of("structure="+structural),elements.stream().map(ApplicationModel.ElementModel::elementId).toList(),pageProvenance);
        ApplicationModel.PageModel page=new ApplicationModel.PageModel(pageId,pageName,identity,title==null?List.of():List.of(title),List.of(structural),List.of(state),new ArrayList<>(regions.values()),elements,List.of(),List.of(),pageProvenance,limitations);
        int skipped=Math.max(0,snapshot.discoveredNodes()-elements.size());ApplicationAggregator.PageScanResult result=new ApplicationAggregator.PageScanResult(page,snapshot.discoveredNodes(),skipped,selectorAnalyses,truncated,List.copyOf(limitations),targets,snapshot.discoveryNanos(),selectorNanos);
        long mergeStart=System.nanoTime();ApplicationAggregator.MergeOutcome merge=aggregator.merge(result);append(limitations,merge.limitations());truncated|=!merge.limitations().isEmpty();if(merge.retained()&&merge.stateRetained()&&pending!=null){ApplicationAggregator.MergeOutcome transition=recordTransition(pending,pageId,stateId,pageProvenance);append(limitations,transition.limitations());truncated|=!transition.limitations().isEmpty();}pending=null;long mergeNanos=System.nanoTime()-mergeStart;
        if(merge.retained()){currentPageId=pageId;currentStateId=merge.stateRetained()?stateId:null;lastTargets=Map.copyOf(targets);lastElements=elements.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(ApplicationModel.ElementModel::elementId,e->e));}
        ApplicationModel model=aggregator.model();metrics=new MappingMetrics(metrics.discoveryNanos()+snapshot.discoveryNanos(),metrics.selectorAnalysisNanos()+selectorNanos,metrics.mergeNanos()+mergeNanos,metrics.discoveryScriptCalls()+1,metrics.seleniumCommands()+seleniumCommands,metrics.observations()+1,metrics.discoveredNodes()+snapshot.discoveredNodes(),metrics.analyzedElements()+elements.size(),metrics.skippedElements()+skipped,metrics.selectorAnalyses()+selectorAnalyses,model.pages().size(),model.coverage().states(),model.transitions().size());
        ApplicationModel.Completeness completeness=truncated?ApplicationModel.Completeness.PARTIAL:ApplicationModel.Completeness.COMPLETE_FOR_OBSERVED_CONTEXT;
        return new MappingObservation(observationId,pageId,stateId,elements.stream().map(ApplicationModel.ElementModel::elementId).toList(),completeness,List.copyOf(limitations),metrics);
    }

    public void beginTransition(String elementId,ApplicationModel.Action action){if(currentPageId==null)throw new IllegalStateException("Observe the source page first");if(currentStateId==null)throw new MappingException(MappingException.Code.PAGE_STATE_UNRETAINED,"The current page state was not retained; transition provenance would be incomplete");if(!lastElements.containsKey(elementId))throw new IllegalArgumentException("Unknown current-page elementId: "+elementId);pending=new PendingTransition(currentPageId,currentStateId,elementId,Objects.requireNonNull(action));}
    public int safeExplore(){if(options.mode()!=ApplicationMapperOptions.Mode.SAFE_EXPLORE)throw new IllegalStateException("SAFE_EXPLORE mode is not enabled");if(options.maxCrawlDepth()==0)return 0;if(currentPageId==null)observe();if(options.maxCrawlDepth()>1)aggregator.limitation("SAFE_EXPLORE_ONE_HOP","V1 safe exploration restores the observed source after each allowed action and does not traverse beyond one hop");int executed=0;String sourceUrl=safe(driver::getCurrentUrl,"current URL"),sourceOrigin=origin(sourceUrl);Set<String>processed=new HashSet<>();
        while(executed<options.maxExploreActions()){
            String id=lastTargets.keySet().stream().filter(candidate->!processed.contains(candidate)).findFirst().orElse(null);if(id==null)break;processed.add(id);
            PageScanner.DiscoveredElement target=lastTargets.get(id);ApplicationModel.ElementModel model=lastElements.get(id);if(target==null||model==null||model.type()!=ApplicationModel.ElementType.LINK||target.targetUrl()==null||opensNewWindow(target.target())||!Objects.equals(sourceOrigin,origin(target.targetUrl())))continue;
            String targetPattern=UrlPatternNormalizer.normalize(target.targetUrl(),options.redactionPolicy());if(options.overrides().deniedUrlPatterns().contains(targetPattern))continue;
            String safeTarget=options.redactionPolicy().redactUrl(target.targetUrl());ApplicationMapperOptions.SafeAction action=new ApplicationMapperOptions.SafeAction(currentPageId,id,model.semanticName(),model.type().name(),safeTarget);ApplicationMapperOptions.ActionDecision decision=options.actionPolicy().evaluate(action);if(decision!=ApplicationMapperOptions.ActionDecision.ALLOW)continue;
            long historyBefore=historyLength();boolean historyEntryCreated=false,restoreNavigationCompleted=false,restored=false;beginTransition(id,ApplicationModel.Action.OPEN);try{target.target().click();long historyAfter=historyLength();historyEntryCreated=createdHistoryEntry(historyBefore,historyAfter);observe();executed++;restoreSource(historyEntryCreated,sourceUrl);restoreNavigationCompleted=true;observe();restored=true;}
            catch(RuntimeException failure){pending=null;if(!historyEntryCreated)historyEntryCreated=createdHistoryEntry(historyBefore,historyLength());RuntimeException cleanupFailure=null;if(!restored)try{if(!restoreNavigationCompleted){restoreSource(historyEntryCreated,sourceUrl);restoreNavigationCompleted=true;}observe();restored=true;}catch(RuntimeException cleanup){cleanupFailure=cleanup;}if(cleanupFailure!=null)failure.addSuppressed(cleanupFailure);if(failure instanceof MappingException mapping)throw mapping;throw new MappingException(MappingException.Code.CRAWL_ACTION_BLOCKED,"Allowed exploration action failed for "+id,failure);}
            finally{pending=null;}}
        return executed;}
    public ApplicationModel model(){return aggregator.model();}public MappingMetrics metrics(){return metrics;}

    private Map<String,ApplicationModel.Region>regions(PageScanner.Snapshot snapshot,String pageId,String context){Map<String,ApplicationModel.Region>out=new TreeMap<>();int count=0;for(PageScanner.DiscoveredRegion region:snapshot.regions()){if(count++>=options.maxRegions())break;String name=redact(first(region.name(),region.role(),region.tag()));String fingerprint=ApplicationIds.id("region-fingerprint-v1",region.role(),name);String id=ApplicationIds.id("region-v1",pageId,fingerprint);out.put(region.key(),new ApplicationModel.Region(id,SemanticNaming.javaIdentifier(name,"Region"),redact(region.role()),fingerprint,ApplicationModel.Provenance.observed("region",context,List.of("NEAREST_MEANINGFUL_REGION"))));}return out;}
    private static String regionId(Map<String,ApplicationModel.Region>regions,String key){if(key==null)return null;ApplicationModel.Region r=regions.get(key);return r==null?null:r.regionId();}
    private static String regionFingerprint(Map<String,ApplicationModel.Region>regions,String key){if(key==null)return null;ApplicationModel.Region r=regions.get(key);return r==null?null:r.fingerprint();}
    private List<ApplicationModel.ElementModel>resolveNames(List<ElementDraft>drafts){Map<String,List<ElementDraft>>groups=new TreeMap<>();drafts.forEach(d->groups.computeIfAbsent(d.model.semanticName(),x->new ArrayList<>()).add(d));List<ApplicationModel.ElementModel>out=new ArrayList<>();for(var group:groups.values()){if(group.size()==1){out.add(group.get(0).model);continue;}group.sort(Comparator.comparing(d->d.model.fingerprint()));for(ElementDraft draft:group){String suffix=draft.model.fingerprint().substring(draft.model.fingerprint().length()-8);out.add(rename(draft.model,draft.model.semanticName()+SemanticNaming.javaIdentifier(suffix,"Target")));}}return out;}
    private static ApplicationModel.ElementModel rename(ApplicationModel.ElementModel e,String name){return new ApplicationModel.ElementModel(e.elementId(),name,e.type(),e.semanticRole(),e.label(),e.accessibleName(),e.actions(),e.preferredSelector(),e.alternativeSelectors(),e.selectorQuality(),e.regionId(),e.fingerprint(),e.provenance(),e.limitations());}
    private ApplicationAggregator.MergeOutcome recordTransition(PendingTransition p,String targetPage,String targetState,ApplicationModel.Provenance provenance){String id=ApplicationIds.id("transition-v1",p.pageId,p.stateId==null?"":p.stateId,p.elementId,p.action.name(),targetPage,targetState);return aggregator.transition(new ApplicationModel.Transition(id,p.pageId,p.stateId,p.elementId,p.action,targetPage,targetState,ApplicationModel.TransitionConfidence.OBSERVED,provenance));}
    private String context(String pattern){String window;try{window=driver.getWindowHandle();}catch(WebDriverException failure){window="unknown";}return "window="+ApplicationIds.id("window-v1",window)+";context=current;url="+pattern;}
    private String redact(String value){return value==null?null:options.redactionPolicy().redact(value);}
    private static String safe(Value call,String label){try{return call.get();}catch(WebDriverException failure){throw new MappingException(MappingException.Code.BROWSER_SCRIPT_FAILED,"Unable to read "+label,failure);}}
    private static String path(String pattern){try{return new URI(pattern).getPath();}catch(Exception ignored){return pattern;}}
    private static String origin(String value){try{URI uri=new URI(value);return uri.getScheme()+"://"+uri.getHost()+":"+(uri.getPort()<0?uri.getScheme().equals("https")?443:80:uri.getPort());}catch(Exception failure){return "";}}
    private static boolean opensNewWindow(org.openqa.selenium.WebElement target){try{String value=target.getAttribute("target");return value!=null&&!value.isBlank()&&!"_self".equalsIgnoreCase(value);}catch(WebDriverException failure){return true;}}
    private long historyLength(){if(!(driver instanceof org.openqa.selenium.JavascriptExecutor js))return -1;try{Object value=js.executeScript("return window.history.length;");return value instanceof Number number?number.longValue():-1;}catch(WebDriverException failure){return -1;}}
    static boolean createdHistoryEntry(long before,long after){return before>=0&&after>before;}
    private void restoreSource(boolean historyEntryCreated,String sourceUrl){if(historyEntryCreated)driver.navigate().back();else driver.navigate().to(sourceUrl);}
    boolean transitionPending(){return pending!=null;}
    private static void append(List<ApplicationModel.Limitation>target,List<ApplicationModel.Limitation>values){for(ApplicationModel.Limitation value:values)if(!target.contains(value))target.add(value);}
    private static String first(String...values){for(String value:values)if(value!=null&&!value.isBlank())return value;return "unknown";}
    private static String stateName(List<ApplicationModel.ElementModel>elements,String structural){if(elements.stream().anyMatch(e->e.type()==ApplicationModel.ElementType.DIALOG))return "dialogOpen";return "state"+structural.substring(structural.length()-8);}
    private static String pageName(String configured){String value=SemanticNaming.javaIdentifier(configured,"Page");return value.endsWith("Page")?value:value+"Page";}
    private static String nameSource(PageScanner.DiscoveredElement e){if(e.testAttributes()!=null&&!e.testAttributes().isEmpty())return "TEST_ATTRIBUTE";if(e.accessibleName()!=null&&!e.accessibleName().isBlank())return "ACCESSIBLE_NAME";if(e.label()!=null&&!e.label().isBlank())return "LABEL";return "FALLBACK";}
    private String elementIdentityBase(PageScanner.DiscoveredElement element,String regionFingerprint){String identityName=first(redact(element.accessibleName()),redact(element.label()),redact(element.role()),SemanticNaming.type(element).name());return ApplicationIds.id("element-identity-v1",SemanticNaming.type(element).name(),identityName,regionFingerprint==null?"":regionFingerprint);}
    private static String observationId(String pageId,PageScanner.Snapshot snapshot,int sequence){return ApplicationIds.id("observation-target-v1",pageId,Integer.toString(snapshot.discoveredNodes()),Integer.toString(sequence));}
    private record ElementDraft(ApplicationModel.ElementModel model,PageScanner.DiscoveredElement target){}
    private record PendingTransition(String pageId,String stateId,String elementId,ApplicationModel.Action action){}
    @FunctionalInterface private interface Value{String get();}
}
