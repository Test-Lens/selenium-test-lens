package io.github.testlens.selector.live;

import io.github.testlens.selector.engine.*;
import org.openqa.selenium.*;

import java.util.*;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;

/** Synchronous caller-thread analysis. It never performs actions or mutates the DOM. */
public final class LiveCandidateAnalysisService {
    private static final String SNAPSHOT_SCRIPT="""
        var t=arguments[0],names=arguments[1],maxDepth=3,maxTokens=8,maxString=2048;
        function cut(v,n){if(v==null)return null;return Array.from(String(v)).slice(0,n).join('');}
        function owned(n){if(!n||n.nodeType!==1)return false;return n.id==='selenium-overlay-host'||n.hasAttribute('data-test-lens-owned')||!!n.closest('#selenium-overlay-host,[data-test-lens-owned]');}
        function attrs(n){var o={};for(var i=0;i<names.length;i++){var v=n.getAttribute(names[i]);if(v!==null)o[names[i]]=cut(v,maxString);}return o;}
        function info(n,d){return {depth:d,tagName:cut((n.tagName||'').toLowerCase(),128),id:cut(n.getAttribute('id'),maxString),classList:Array.from(n.classList||[]).slice(0,maxTokens).map(x=>cut(x,maxString)),testAttributes:attrs(n)};}
        var excluded=0,ancestors=[],p=t.parentElement,depth=1;
        while(p&&depth<=maxDepth){if(owned(p)){excluded++;p=p.parentElement;continue;}ancestors.push(info(p,depth++));p=p.parentElement;}
        var ownedNodes=[],host=document.getElementById('selenium-overlay-host');
        if(host){ownedNodes.push(host);var root=host.shadowRoot||host;Array.from(root.querySelectorAll('*')).slice(0,256).forEach(x=>ownedNodes.push(x));}
        Array.from(document.querySelectorAll('[data-test-lens-owned]')).slice(0,Math.max(0,256-ownedNodes.length)).forEach(x=>ownedNodes.push(x));
        return {tagName:cut((t.tagName||'').toLowerCase(),128),id:cut(t.getAttribute('id'),maxString),name:cut(t.getAttribute('name'),maxString),classList:Array.from(t.classList||[]).slice(0,maxTokens).map(x=>cut(x,maxString)),testAttributes:attrs(t),ariaLabel:cut(t.getAttribute('aria-label'),256),ancestors:ancestors,ownedNodes:ownedNodes,instrumentationNodesExcluded:excluded,truncated:false};
        """;
    private final CandidateGenerator generator=new CandidateGenerator();
    private final CandidateRanker ranker=new CandidateRanker();

    public CandidateAnalysis analyze(LiveCandidateRequest request){
        Objects.requireNonNull(request,"request");
        if(request.target()==null)return empty(request,Recommendation.TARGET_REQUIRED,"TARGET_REQUIRED","A trustworthy live target is required");
        if(request.searchContext()==null)return empty(request,Recommendation.NO_VALID_CANDIDATE,"CONTEXT_UNAVAILABLE","A live SearchContext is required");
        SnapshotCapture captured;
        try{captured=snapshot(request);}catch(StaleElementReferenceException stale){return stale(request);}catch(WebDriverException failure){return empty(request,Recommendation.NO_VALID_CANDIDATE,"METADATA_CAPTURE_FAILED",failure.getClass().getSimpleName());}
        Locator original=request.originalBy()==null?null:SeleniumLocatorAdapter.structural(request.originalBy());
        CandidateGenerator.Request generation=new CandidateGenerator.Request(original,request.additionalPreferredTestAttributes(),request.contextFingerprint(),request.declarationRef(),request.modulePath(),request.logicalPath(),request.declaringSymbol(),request.usageClass(),request.usageMethod(),request.policies(),request.evidence());
        CandidateGenerator.Generated generated=generator.generate(captured.snapshot,generation);
        List<Candidate> candidates=new ArrayList<>(generated.candidates());Map<String,By> executable=new HashMap<>();
        String originalId=original==null?null:CandidateIds.id(original,request.contextFingerprint());
        for(Candidate candidate:candidates){
            if(candidate.original()&&request.originalBy()!=null){executable.put(candidate.candidateId(),request.originalBy());continue;}
            try{executable.put(candidate.candidateId(),SeleniumLocatorAdapter.toBy(candidate.locator()));}catch(IllegalArgumentException ignored){/* represented as unsupported below */}
        }
        if(request.originalBy()!=null&&original==null){Candidate opaque=opaqueOriginal();candidates.add(0,opaque);executable.put(opaque.candidateId(),request.originalBy());originalId=opaque.candidateId();}
        List<Candidate> validated=new ArrayList<>();List<Issue> issues=new ArrayList<>();int commands=0,excluded=captured.snapshot.instrumentationNodesExcluded();boolean sessionLost=false;
        for(Candidate candidate:candidates){
            if(sessionLost){validated.add(candidate.withValidation(new Validation(ValidationState.SESSION_LOST,-1,TargetComparison.UNKNOWN,List.of("SESSION_LOST"))));continue;}
            if(request.shadowContext()&&candidate.locator()!=null&&"xpath".equals(candidate.locator().strategy())){validated.add(candidate.withValidation(new Validation(ValidationState.UNSUPPORTED,-1,TargetComparison.UNKNOWN,List.of("XPATH_UNSUPPORTED_IN_SHADOW_CONTEXT"))));continue;}
            if(!executable.containsKey(candidate.candidateId())){validated.add(candidate.withValidation(new Validation(ValidationState.UNSUPPORTED,-1,TargetComparison.UNKNOWN,List.of("UNSUPPORTED_SELENIUM_STRATEGY"))));continue;}
            try{
                commands++;List<WebElement> matches=request.searchContext().findElements(executable.get(candidate.candidateId()));
                List<WebElement> application=new ArrayList<>();for(WebElement match:matches){if(contains(captured.instrumentationNodes,match))excluded++;else application.add(match);}
                validated.add(candidate.withValidation(validate(application,request.target(),request.usageIntent())));
            }catch(InvalidSelectorException invalid){validated.add(candidate.withValidation(new Validation(ValidationState.INVALID_SELECTOR,-1,TargetComparison.UNKNOWN,List.of(invalid.getClass().getSimpleName()))));}
            catch(NoSuchSessionException lost){sessionLost=true;validated.add(candidate.withValidation(new Validation(ValidationState.SESSION_LOST,-1,TargetComparison.UNKNOWN,List.of("SESSION_LOST"))));issues.add(new Issue("SESSION_LOST",lost.getClass().getSimpleName(),true));}
            catch(StaleElementReferenceException stale){validated.add(candidate.withValidation(new Validation(ValidationState.STALE_TARGET,-1,TargetComparison.STALE_TARGET,List.of("STALE_TARGET"))));}
            catch(WebDriverException failure){validated.add(candidate.withValidation(new Validation(ValidationState.NOT_LIVE_VALIDATED,-1,TargetComparison.UNKNOWN,List.of(failure.getClass().getSimpleName()))));}
        }
        List<Candidate> ranked=ranker.rank(validated);Recommendation recommendation=ranker.recommend(ranked,originalId,true);
        Completeness completeness=new Completeness(true,captured.snapshot.truncated(),generated.candidateLimitReached(),excluded,generated.generatedBeforeDedup(),candidates.size(),commands);
        return new CandidateAnalysis(SCHEMA_VERSION,request.usageIntent(),request.contextFingerprint(),request.declarationRef(),request.logicalPath(),originalId,ranked,recommendation,completeness,issues);
    }

    private SnapshotCapture snapshot(LiveCandidateRequest request){
        if(!(request.driver() instanceof JavascriptExecutor js))throw new WebDriverException("JavascriptExecutor unavailable");
        Object raw=js.executeScript(SNAPSHOT_SCRIPT,request.target(),request.additionalPreferredTestAttributes());
        if(!(raw instanceof Map<?,?> map))throw new WebDriverException("Unexpected snapshot response");
        String text=safeCall(request.target()::getText),accessible=safeCall(request.target()::getAccessibleName),role=safeCall(request.target()::getAriaRole);
        List<String> classes=strings(map.get("classList"),TargetSnapshot.MAX_CLASS_TOKENS);Map<String,String> attrs=stringMap(map.get("testAttributes"),request.additionalPreferredTestAttributes());
        List<TargetSnapshot.AncestorHint> ancestors=new ArrayList<>();if(map.get("ancestors") instanceof List<?> list)for(Object value:list){if(!(value instanceof Map<?,?> a))continue;int depth=number(a.get("depth"),ancestors.size()+1);if(depth>3)continue;ancestors.add(new TargetSnapshot.AncestorHint(depth,string(a.get("tagName")),string(a.get("id")),stringMap(a.get("testAttributes"),request.additionalPreferredTestAttributes()),strings(a.get("classList"),8)));}
        int estimated=estimate(string(map.get("tagName")),string(map.get("id")),string(map.get("name")),text,accessible,role)
                +estimate(classes)+estimate(attrs)+estimate(ancestors);boolean truncated=Boolean.TRUE.equals(map.get("truncated"))||estimated>TargetSnapshot.MAX_SNAPSHOT_BYTES;
        TargetSnapshot snapshot=truncated
                ?new TargetSnapshot(string(map.get("tagName")),string(map.get("id")),string(map.get("name")),List.of(),Map.of(),null,null,null,List.of(),number(map.get("instrumentationNodesExcluded"),0),true)
                :new TargetSnapshot(string(map.get("tagName")),string(map.get("id")),string(map.get("name")),classes,attrs,text,accessible,role,ancestors,number(map.get("instrumentationNodesExcluded"),0),false);
        List<WebElement> owned=new ArrayList<>();if(map.get("ownedNodes") instanceof List<?> list)for(Object value:list)if(value instanceof WebElement element)owned.add(element);
        return new SnapshotCapture(snapshot,List.copyOf(owned));
    }
    private static Validation validate(List<WebElement> matches,WebElement target,UsageIntent intent){int count=matches.size();if(count==0)return new Validation(ValidationState.NO_MATCH,0,TargetComparison.TARGET_UNAVAILABLE,List.of());boolean found=contains(matches,target);TargetComparison comparison=found?TargetComparison.SAME_TARGET:TargetComparison.DIFFERENT_TARGET;if(!found)return new Validation(ValidationState.WRONG_TARGET,count,comparison,List.of());if(intent==UsageIntent.FIND_MANY)return new Validation(ValidationState.VALID_FOR_INTENT,count,comparison,List.of());if(intent==UsageIntent.FIND_ONE)return new Validation(count==1?ValidationState.VERIFIED_IN_SCOPE:ValidationState.VALID_BUT_AMBIGUOUS,count,comparison,List.of());return new Validation(ValidationState.VALID_FOR_INTENT,count,comparison,List.of("UNIQUENESS_NOT_REQUIRED_FOR_UNKNOWN_INTENT"));}
    private static boolean contains(List<WebElement> values,WebElement target){WebElement expected=unwrap(target);for(WebElement value:values){WebElement actual=unwrap(value);if(actual==expected||actual.equals(expected)||expected.equals(actual))return true;}return false;}
    private static WebElement unwrap(WebElement value){WebElement current=value;Set<WebElement> seen=Collections.newSetFromMap(new IdentityHashMap<>());while(current instanceof WrapsElement wraps&&seen.add(current)){WebElement next=wraps.getWrappedElement();if(next==null||next==current)break;current=next;}return current;}
    private static Candidate opaqueOriginal(){return new Candidate(CandidateIds.opaqueOriginalId(),null,true,List.of(Origin.ORIGINAL,Origin.CUSTOM),List.of(),Validation.notLive(),new Complexity(ScopeFragility.DIRECT,SemanticPreference.GENERIC_TEXT_OR_TAG,0,0,0,0),List.of(),List.of("OPAQUE_ORIGINAL_NOT_SERIALIZABLE"),true);}
    private static CandidateAnalysis empty(LiveCandidateRequest r,Recommendation recommendation,String code,String detail){return new CandidateAnalysis(SCHEMA_VERSION,r.usageIntent(),r.contextFingerprint(),r.declarationRef(),r.logicalPath(),null,List.of(),recommendation,new Completeness(false,false,false,0,0,0,0),List.of(new Issue(code,detail,false)));}
    private static CandidateAnalysis stale(LiveCandidateRequest r){return empty(r,Recommendation.REVIEW_REQUIRED,"STALE_TARGET","Target became stale during metadata capture");}
    private static String safeCall(Call call){try{return call.get();}catch(StaleElementReferenceException stale){throw stale;}catch(WebDriverException ignored){return null;}}
    private static String string(Object value){return value==null?null:String.valueOf(value);}
    private static int number(Object value,int fallback){return value instanceof Number n?n.intValue():fallback;}
    private static List<String> strings(Object value,int max){if(!(value instanceof List<?> list))return List.of();return list.stream().filter(Objects::nonNull).map(String::valueOf).sorted().limit(max).toList();}
    private static Map<String,String> stringMap(Object value,List<String> allowed){if(!(value instanceof Map<?,?> map))return Map.of();Map<String,String> out=new TreeMap<>();for(String key:allowed){Object entry=map.get(key);if(entry!=null)out.put(key,String.valueOf(entry));}return Map.copyOf(out);}
    private static int estimate(String... values){int size=0;for(String value:values)if(value!=null)size+=value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;return size;}
    private static int estimate(Collection<?> values){int size=0;for(Object value:values)size+=estimate(String.valueOf(value));return size;}
    private static int estimate(Map<String,String> values){int size=0;for(Map.Entry<String,String> value:values.entrySet())size+=estimate(value.getKey(),value.getValue());return size;}
    private record SnapshotCapture(TargetSnapshot snapshot,List<WebElement> instrumentationNodes) { }
    @FunctionalInterface private interface Call{String get();}
}
