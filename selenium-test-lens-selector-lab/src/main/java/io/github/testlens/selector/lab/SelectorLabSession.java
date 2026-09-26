package io.github.testlens.selector.lab;

import io.github.testlens.selector.engine.*;
import io.github.testlens.selector.live.LiveCandidateAnalysis;
import io.github.testlens.selector.live.LiveCandidateAnalysisService;
import io.github.testlens.selector.live.LiveCandidateRequest;
import org.openqa.selenium.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Internal blocking read-only Lab session. Every WebDriver command runs on the calling thread.
 * No picker event is dispatched to the selected application element and no default action of that element is executed.
 * A pre-existing window/document capture listener may still observe an event targeting Test Lens instrumentation.
 */
public final class SelectorLabSession {
    public static final int PROTOCOL_VERSION=1,MAX_QUEUE=16,MAX_PAYLOAD_BYTES=16*1024,MAX_SIMILAR_RESULTS=100;
    private static final Duration WAIT=Duration.ofSeconds(30),LAB_SCRIPT_TIMEOUT=Duration.ofSeconds(35);
    private final SelectorLabRequest request;private final JavascriptExecutor js;private final LiveCandidateAnalysisService live=new LiveCandidateAnalysisService();
    private final SelectorLabPolicyFeedback feedback;
    private final String sessionRef="selector-lab-session:"+UUID.randomUUID();private String generation=sessionRef+":document:1";
    private SelectorLabState state=SelectorLabState.CLOSED;private long lastSequence;private int analysisSequence,accepted,rejected;
    private LiveCandidateAnalysis current;private boolean stop;private final List<String>issues=new ArrayList<>();
    private final Map<String,PatternState>patterns=new HashMap<>();private PolicyDraft.PendingPolicyChange pending;

    public SelectorLabSession(SelectorLabRequest request){this.request=Objects.requireNonNull(request,"request");if(!(request.driver() instanceof JavascriptExecutor executor))throw new IllegalArgumentException("Selector Lab requires JavascriptExecutor");this.js=executor;this.feedback=request.policyWorkspace()==null?null:new SelectorLabPolicyFeedback(request.policyWorkspace());}

    /** Runs until explicit close, document replacement, or fatal session loss. */
    public SelectorLabResult run(){
        Duration original=request.driver().manage().timeouts().getScriptTimeout();boolean changed=false,restored=true;
        try{
            if(original.compareTo(WAIT.plusSeconds(2))<=0){request.driver().manage().timeouts().scriptTimeout(LAB_SCRIPT_TIMEOUT);changed=true;}
            open();
            while(!stop){Object raw=awaitEvent();if(!(raw instanceof Map<?,?>event)){reject("MALFORMED_EVENT");continue;}dispatch(event);}
        }catch(NoSuchSessionException lost){issues.add("SESSION_LOST");state=SelectorLabState.ERROR;stop=true;}
        catch(TimeoutException timeout){issues.add("ASYNC_PROVIDER_TIMEOUT");state=SelectorLabState.ERROR;stop=true;}
        catch(JavascriptException navigation){issues.add("DOCUMENT_CHANGED");invalidateDocument();stop=true;}
        catch(WebDriverException failure){issues.add("WEBDRIVER_ERROR:"+failure.getClass().getSimpleName());state=SelectorLabState.ERROR;stop=true;}
        finally{
            closeBrowser();clearCurrent();if(changed)try{request.driver().manage().timeouts().scriptTimeout(original);}catch(WebDriverException failure){restored=false;issues.add("SCRIPT_TIMEOUT_RESTORE_FAILED:"+failure.getClass().getSimpleName());}
            state=SelectorLabState.CLOSED;
        }
        return new SelectorLabResult(state,issues,restored,accepted,rejected,pending);
    }

    public SelectorLabState state(){return state;}
    private void open(){transition(SelectorLabState.IDLE);Object ok=js.executeScript(SelectorLabJs.INIT+SelectorLabJs.BRIDGE+"return lab.open(arguments[0],arguments[1],arguments[2]);",sessionRef,generation,request.displayMode().name());if(!Boolean.TRUE.equals(ok))throw new WebDriverException("Lab UI initialization failed");}
    private Object awaitEvent(){return js.executeAsyncScript(SelectorLabJs.BRIDGE+"var done=arguments[arguments.length-1];if(!lab){done({kind:'DOCUMENT_CHANGED'});return;}lab.awaitEvent(done,arguments[0]);",WAIT.toMillis());}

    private void dispatch(Map<?,?>event){
        String kind=text(event.get("kind"));if("WAIT_TIMEOUT".equals(kind))return;if("DOCUMENT_CHANGED".equals(kind)){issues.add("DOCUMENT_CHANGED");invalidateDocument();stop=true;return;}
        if(!validEnvelope(event)){reject("INVALID_ENVELOPE");return;}
        if("PICK_CANCELLED".equals(kind)){if(state==SelectorLabState.PICKING)transition(SelectorLabState.IDLE);return;}
        if("TARGET_SELECTION".equals(kind)){selection(event);return;}
        if(!"COMMAND".equals(kind)){reject("UNSUPPORTED_EVENT_KIND");return;}
        String type=text(event.get("commandType"));
        switch(type){
            case"START_PICK","REPICK"->startPick();
            case"HIGHLIGHT_CANDIDATE"->highlight(text(event.get("analysisId")),text(event.get("candidateId")));
            case"FIND_SIMILAR"->findSimilar(text(event.get("analysisId")),text(event.get("candidateId")));
            case"PREVIEW_PATTERN"->preview(text(event.get("analysisId")),text(event.get("candidateId")));
            case"USE_ONCE"->useOnce(text(event.get("analysisId")),text(event.get("candidateId")));
            case"PREVIEW_EXACT_POLICY"->prepareExact(event);
            case"PREVIEW_PATTERN_POLICY"->preparePattern(event);
            case"PREPARE_REMOVE_RULE"->prepareRemove(event);
            case"PREPARE_REPLACE_RULE"->prepareReplace(event);
            case"SET_POLICY_SCOPE","SET_POLICY_DESTINATION"->reject("PREVIEW_REGENERATION_REQUIRED");
            case"CANCEL_DRAFT"->{if(feedback!=null)feedback.cancelDraft();publishStatus("READY","Prepared policy change cancelled");accepted++;}
            case"TRANSFER_PENDING_CHANGE"->transfer(event);
            case"CLOSE"->{accepted++;stop=true;}
            default->reject("UNSUPPORTED_COMMAND");
        }
    }
    private boolean validEnvelope(Map<?,?>event){
        if(number(event.get("protocolVersion"))!=PROTOCOL_VERSION||!sessionRef.equals(text(event.get("sessionRef")))||!generation.equals(text(event.get("documentGeneration"))))return false;
        long sequence=longNumber(event.get("sequence"));if(sequence<=lastSequence)return false;
        if(String.valueOf(event.get("payload")).getBytes(StandardCharsets.UTF_8).length>MAX_PAYLOAD_BYTES)return false;
        lastSequence=sequence;return true;
    }
    private void startPick(){if(state!=SelectorLabState.IDLE&&state!=SelectorLabState.READY&&state!=SelectorLabState.TARGET_STALE){reject("ILLEGAL_PICK_TRANSITION");return;}clearCurrent();js.executeScript(SelectorLabJs.BRIDGE+"return !!(lab&&lab.startPick());");transition(SelectorLabState.PICKING);accepted++;}
    private void selection(Map<?,?>event){
        if(state!=SelectorLabState.PICKING||!(event.get("element") instanceof WebElement target)){reject("INVALID_TARGET_SELECTION");return;}
        transition(SelectorLabState.TARGET_SELECTED);SearchContext context=request.searchContext();
        if(event.get("shadowHosts") instanceof List<?>hosts){if(hosts.size()>8){reject("SHADOW_DEPTH_EXCEEDED");transition(SelectorLabState.ERROR);return;}for(Object value:hosts){if(!(value instanceof WebElement host)){reject("INVALID_SHADOW_HOST");transition(SelectorLabState.ERROR);return;}context=host.getShadowRoot();}}
        transition(SelectorLabState.ANALYZING);String analysisId=sessionRef+":analysis:"+(++analysisSequence);
        LiveCandidateRequest liveRequest=new LiveCandidateRequest(request.driver(),context,target,request.usageIntent(),request.originalBy(),request.contextFingerprint(),!Objects.equals(context,request.searchContext()),request.declarationRef(),request.modulePath(),request.logicalPath(),request.declaringSymbol(),request.usageClass(),request.usageMethod(),request.policies(),request.evidence(),request.preferredTestAttributes());
        current=live.analyzeRetainingMatches(liveRequest,analysisId,generation);
        if(feedback!=null)feedback.beginAnalysis(analysisId);patterns.clear();
        if(current.analysis().issues().stream().anyMatch(i->"STALE_TARGET".equals(i.code()))){transition(SelectorLabState.TARGET_STALE);publishStatus("TARGET_STALE","Pick again");}
        else{transition(SelectorLabState.READY);publishAnalysis();}accepted++;
    }
    private void highlight(String analysisId,String candidateId){
        CandidateAnalysis.Candidate candidate=known(analysisId,candidateId);if(candidate==null)return;
        List<WebElement>matches=current.retainedMatches(candidateId);try{Object count=js.executeScript(SelectorLabJs.BRIDGE+"return lab.highlight(arguments[0],arguments[1]);",matches,current.target());publishStatus("READY",number(count)+" highlighted of "+candidate.validation().matchCount()+(current.matchesNotRetainedForHighlight(candidateId)?" (retention truncated)":""));accepted++;}catch(StaleElementReferenceException stale){js.executeScript(SelectorLabJs.BRIDGE+"lab.clearHighlights();");publishStatus("TARGET_STALE","Retained match became stale; re-analysis required");transition(SelectorLabState.TARGET_STALE);accepted++;}
    }
    private void findSimilar(String analysisId,String candidateId){CandidateAnalysis.Candidate candidate=known(analysisId,candidateId);if(candidate==null)return;if(candidate.locator()==null||request.similarityCatalog().isEmpty()){renderMessage("Find Similar",List.of("No supplied catalog or scalar candidate"));accepted++;return;}SimilaritySubject source=similarity(candidate);SimilarityResult result=new SimilarityIndex(request.similarityCatalog()).find(source,request.similarityScope(),request.policies(),request.similarityEvidence());List<String>lines=result.relatedSubjects().stream().limit(MAX_SIMILAR_RESULTS).map(r->r.relationClass()+" · "+safe(r.logicalPath())+" · "+r.explanation()+" · similar does not mean same target").toList();renderMessage("Find Similar · "+result.relatedSubjects().size()+" results",lines);accepted++;}
    private void preview(String analysisId,String candidateId){CandidateAnalysis.Candidate candidate=known(analysisId,candidateId);if(candidate==null)return;if(candidate.locator()==null){renderMessage("Pattern preview",List.of("Opaque candidate is unsupported"));accepted++;return;}SelectorSubject subject=subject(candidate);PatternProposal.Result proposal=new PatternProposal().propose(subject);FederatedPatternPreview.Result result=new FederatedPatternPreview().preview(proposal,request.similarityCatalog(),request.policies(),request.similarityEvidence(),request.incompleteHistory());List<String>lines=new ArrayList<>();if(!proposal.useful())lines.add("No conservative detector-backed proposal");else{String proposalId="selector-pattern-proposal-v1:sha256:"+CanonicalDigests.digest("selector-pattern-proposal-v1",candidateId,proposal.pattern().toString());patterns.put(candidateId,new PatternState(proposalId,proposal,result));lines.add("proposalId: "+proposalId);lines.add("segments: "+proposal.pattern().segments().stream().map(s->s.kind().name()).toList());lines.add("static matches: "+result.matchedStaticSubjects().size());lines.add("history matches: "+result.matchedHistorySubjects().size());lines.add("excluded: "+result.excludedNearMatches().size());lines.add("unsupported digest-only: "+result.unsupportedDigestOnlySubjects().size());lines.add("policy conflicts: "+result.policyConflicts().size());lines.add("evidence conflicts: "+result.evidenceConflicts().size());}renderMessage("Pattern preview · read-only",lines);accepted++;}
    private void useOnce(String analysisId,String candidateId){CandidateAnalysis.Candidate candidate=known(analysisId,candidateId);if(candidate==null)return;if(feedback==null){reject("POLICY_FEEDBACK_UNAVAILABLE");return;}try{SessionCandidateFeedback value=feedback.useOnce(analysisId,candidate);publishStatus("READY","Chosen for this Lab session"+(value.ambiguityWarning()?"; candidate remains ambiguous":""));accepted++;}catch(IllegalArgumentException failure){reject(failure.getMessage());}}
    private void prepareExact(Map<?,?>event){CandidateAnalysis.Candidate candidate=known(text(event.get("analysisId")),text(event.get("candidateId")));if(candidate==null||feedback==null){if(feedback==null)reject("POLICY_WORKSPACE_UNAVAILABLE");return;}Map<?,?>payload=payload(event);if(!allowed(payload,"decision","scope","destination")||browserAcknowledgement(payload))return;if(secret(candidate)){reject("SECURITY_REDACTED_POLICY_DERIVATION");return;}try{SelectorPolicy.Decision decision=SelectorPolicy.Decision.valueOf(text(payload.get("decision")));PolicyDraft.ScopeChoice scope=PolicyDraft.ScopeChoice.valueOf(text(payload.get("scope")));PolicyWorkspaceSnapshot.Origin destination=PolicyWorkspaceSnapshot.Origin.valueOf(text(payload.get("destination")));publishDraft(feedback.prepareExact(text(event.get("analysisId")),candidate,subject(candidate),decision,scope,destination,request.evidence()));accepted++;}catch(RuntimeException failure){reject("INVALID_POLICY_REQUEST");}}
    private void preparePattern(Map<?,?>event){CandidateAnalysis.Candidate candidate=known(text(event.get("analysisId")),text(event.get("candidateId")));if(candidate==null||feedback==null){if(feedback==null)reject("POLICY_WORKSPACE_UNAVAILABLE");return;}Map<?,?>payload=payload(event);if(!allowed(payload,"patternProposalId","decision","scope","destination")||browserAcknowledgement(payload))return;if(secret(candidate)){reject("SECURITY_REDACTED_POLICY_DERIVATION");return;}PatternState pattern=patterns.get(candidate.candidateId());if(pattern==null||!pattern.proposalId().equals(text(payload.get("patternProposalId")))){reject("UNKNOWN_PATTERN_PROPOSAL_ID");return;}try{SelectorPolicy.Decision decision=SelectorPolicy.Decision.valueOf(text(payload.get("decision")));PolicyDraft.ScopeChoice scope=PolicyDraft.ScopeChoice.valueOf(text(payload.get("scope")));PolicyWorkspaceSnapshot.Origin destination=PolicyWorkspaceSnapshot.Origin.valueOf(text(payload.get("destination")));publishDraft(feedback.preparePattern(text(event.get("analysisId")),candidate,subject(candidate),decision,scope,destination,pattern.proposal(),pattern.preview(),request.evidence(),pattern.proposalId()));accepted++;}catch(RuntimeException failure){reject("INVALID_POLICY_REQUEST");}}
    private void prepareRemove(Map<?,?>event){if(feedback==null){reject("POLICY_WORKSPACE_UNAVAILABLE");return;}Map<?,?>payload=payload(event);if(!allowed(payload,"ruleId")||browserAcknowledgement(payload))return;publishDraft(feedback.prepareRemove(text(payload.get("ruleId"))));accepted++;}
    private void prepareReplace(Map<?,?>event){if(feedback==null){reject("POLICY_WORKSPACE_UNAVAILABLE");return;}Map<?,?>payload=payload(event);if(!allowed(payload,"ruleId","decision","scope")||browserAcknowledgement(payload))return;PolicyWorkspaceSnapshot.OriginRule old=request.policyWorkspace().find(text(payload.get("ruleId")));if(old==null){reject("UNKNOWN_RULE_ID");return;}try{SelectorPolicy.Scope scope=scope(old.rule().scope(),PolicyDraft.ScopeChoice.valueOf(text(payload.get("scope"))));if(scope==null){reject("REQUESTED_SCOPE_UNAVAILABLE");return;}publishDraft(feedback.prepareReplace(old.rule().ruleId(),SelectorPolicy.Decision.valueOf(text(payload.get("decision"))),scope));accepted++;}catch(RuntimeException failure){reject("INVALID_POLICY_REQUEST");}}
    private void publishDraft(PolicyDraftService.Preparation prepared){if(!prepared.prepared()){renderMessage("Policy change unavailable",prepared.issues());return;}PolicyDraft draft=prepared.draft();Map<String,Object>model=new LinkedHashMap<>();model.put("draftId",draft.draftId());model.put("action",draft.action().name());model.put("destination",draft.destination().name());model.put("warnings",draft.warnings().stream().map(Enum::name).toList());model.put("blocking",draft.blockingIssues().stream().map(Enum::name).toList());model.put("acknowledgements",draft.requiredHostAcknowledgements().stream().map(Enum::name).sorted().toList());model.put("transferable",draft.transferable());js.executeScript(SelectorLabJs.BRIDGE+"return lab.renderPolicyDraft(arguments[0]);",model);}
    private void transfer(Map<?,?>event){if(feedback==null){reject("POLICY_WORKSPACE_UNAVAILABLE");return;}Map<?,?>payload=payload(event);if(!allowed(payload,"draftId")||browserAcknowledgement(payload))return;try{pending=feedback.transfer(text(payload.get("draftId")));accepted++;stop=true;}catch(IllegalArgumentException failure){reject(failure.getMessage());}}
    private boolean browserAcknowledgement(Map<?,?>payload){if(payload.containsKey("acknowledged")||payload.containsKey("hostAcknowledgement")||payload.containsKey("hostAcknowledgements")){reject("BROWSER_HOST_ACKNOWLEDGEMENT_FORBIDDEN");return true;}return false;}
    private boolean allowed(Map<?,?>payload,String...names){Set<String>allowed=Set.of(names);for(Object key:payload.keySet())if(!(key instanceof String value)||!allowed.contains(value)){reject("UNSUPPORTED_POLICY_PAYLOAD_FIELD");return false;}return true;}
    private boolean secret(CandidateAnalysis.Candidate candidate){if(candidate.locator()==null||candidate.locator().value()==null)return true;String raw=candidate.locator().value();return !Objects.equals(raw,request.redactionPolicy().redact(raw));}
    private static Map<?,?>payload(Map<?,?>event){return event.get("payload") instanceof Map<?,?>map?map:Map.of();}
    private static SelectorPolicy.Scope scope(SelectorPolicy.Scope old,PolicyDraft.ScopeChoice choice){return switch(choice){case DECLARATION->old.declarationRef()==null?null:new SelectorPolicy.Scope(null,null,old.declarationRef(),null,null,null,null);case FILE->old.logicalPath()==null?null:new SelectorPolicy.Scope(null,old.logicalPath(),null,null,null,null,null);case MODULE->old.modulePath()==null?null:new SelectorPolicy.Scope(old.modulePath(),null,null,null,null,null,null);case PROJECT->SelectorPolicy.Scope.project();case RUNTIME_NARROW->(old.usageClass()==null&&old.usageMethod()==null&&old.contextFingerprint()==null)?null:new SelectorPolicy.Scope(null,null,null,null,old.usageClass(),old.usageMethod(),old.contextFingerprint());};}
    private CandidateAnalysis.Candidate known(String analysisId,String candidateId){if(state!=SelectorLabState.READY||current==null||!Objects.equals(current.analysisId(),analysisId)){reject("STALE_ANALYSIS_ID");return null;}CandidateAnalysis.Candidate candidate=current.analysis().candidates().stream().filter(c->c.candidateId().equals(candidateId)).findFirst().orElse(null);if(candidate==null)reject("UNKNOWN_CANDIDATE_ID");return candidate;}

    private SimilaritySubject similarity(CandidateAnalysis.Candidate c){return SimilaritySubject.create(subject(c),ComponentIdentity.of("TARGET","candidate"),null,List.of(),c.origins().stream().map(Enum::name).toList(),c.complexity().semanticPreference().name(),null,List.of(),new SimilaritySubject.Correlation(SimilaritySubject.Confidence.UNKNOWN,List.of("LIVE_CANDIDATE"),List.of(),List.of("DECLARATION_CORRELATION"),1),SimilaritySubject.SourceState.CURRENT);}
    private SelectorSubject subject(CandidateAnalysis.Candidate c){return new SelectorSubject(SelectorSubject.SubjectKind.RUNTIME_OBSERVATION,c.locator().strategy(),SelectorSubject.ValueState.KNOWN,c.locator().value(),request.declarationRef(),request.modulePath(),request.logicalPath(),request.declaringSymbol(),request.usageClass(),request.usageMethod(),request.contextFingerprint(),SelectorSubject.InputTrust.RUNTIME_RAW_LOCAL,null);}
    private void publishAnalysis(){js.executeScript(SelectorLabJs.BRIDGE+"return lab.renderAnalysis(arguments[0]);",projection());}
    private Map<String,Object>projection(){CandidateAnalysis a=current.analysis();List<Map<String,Object>>candidates=new ArrayList<>();for(CandidateAnalysis.Candidate c:a.candidates()){String raw=c.locator()==null?null:c.locator().value();String redacted=raw==null?null:request.redactionPolicy().redact(raw);boolean secret=raw!=null&&!Objects.equals(raw,redacted);String display=c.locator()==null?"custom opaque locator":c.locator().strategy()+": "+(secret?"[REDACTED]":redacted);String snippet=secret?null:JavaLocatorSnippet.render(c.locator());String stability=c.stabilityComponents().stream().map(x->x.assessment().effectiveDisposition().name()).distinct().sorted().reduce((x,y)->x+", "+y).orElse("INSUFFICIENT_DATA");Map<String,Object>row=new LinkedHashMap<>();row.put("candidateId",c.candidateId());row.put("current",c.original());row.put("display",display);row.put("validation",c.validation().state().name());row.put("matchCount",c.validation().matchCount());row.put("targetComparison",c.validation().targetComparison().name());row.put("stability",stability);row.put("reasons",c.reasons().stream().map(CandidateAnalysis.Reason::code).toList());row.put("limitations",c.limitations());row.put("copySnippet",snippet);candidates.add(row);}Map<String,Object>model=new LinkedHashMap<>();model.put("analysisId",current.analysisId());model.put("state","READY");model.put("recommendation",a.recommendation().name());model.put("summary",a.candidates().size()+" candidates · engine order");model.put("candidates",candidates);model.put("policyFeedbackAvailable",feedback!=null);model.put("defaultPolicyScope",request.declarationRef()!=null?"DECLARATION":(request.usageClass()!=null||request.usageMethod()!=null||request.contextFingerprint()!=null)?"RUNTIME_NARROW":null);model.put("sourceNavigationTarget",request.preparedSourceNavigationTarget());SelectorAuditProjection.Declaration audit=request.auditProjection()==null?null:request.auditProjection().declaration(request.declarationRef());if(audit!=null)model.put("audit",audit.findings().stream().map(f->f.severity()+" "+f.code()).toList());return model;}
    private void renderMessage(String title,List<String>lines){js.executeScript(SelectorLabJs.BRIDGE+"lab.renderMessage(arguments[0],arguments[1]);",title,lines.stream().map(request.redactionPolicy()::redact).toList());}
    private void publishStatus(String value,String message){try{js.executeScript(SelectorLabJs.BRIDGE+"if(lab)lab.setStatus(arguments[0],arguments[1]);",value,message);}catch(WebDriverException ignored){}}
    private void reject(String code){rejected++;issues.add(code);publishStatus(state.name(),"Rejected: "+code);}
    private void invalidateDocument(){clearCurrent();state=SelectorLabState.IDLE;generation=sessionRef+":document:"+(analysisSequence+2);}
    private void clearCurrent(){if(current!=null){current.close();current=null;}patterns.clear();if(feedback!=null)feedback.clear();try{js.executeScript(SelectorLabJs.BRIDGE+"if(lab)lab.clearHighlights();");}catch(WebDriverException ignored){}}
    private void closeBrowser(){try{js.executeScript(SelectorLabJs.BRIDGE+"if(lab)lab.close();");}catch(WebDriverException ignored){}}
    private void transition(SelectorLabState next){boolean allowed=switch(state){case CLOSED->next==SelectorLabState.IDLE;case IDLE->next==SelectorLabState.PICKING||next==SelectorLabState.CLOSED;case PICKING->next==SelectorLabState.TARGET_SELECTED||next==SelectorLabState.IDLE||next==SelectorLabState.ERROR||next==SelectorLabState.CLOSED;case TARGET_SELECTED->next==SelectorLabState.ANALYZING||next==SelectorLabState.ERROR||next==SelectorLabState.CLOSED;case ANALYZING->next==SelectorLabState.READY||next==SelectorLabState.TARGET_STALE||next==SelectorLabState.ERROR||next==SelectorLabState.CLOSED;case READY->next==SelectorLabState.PICKING||next==SelectorLabState.IDLE||next==SelectorLabState.TARGET_STALE||next==SelectorLabState.ERROR||next==SelectorLabState.CLOSED;case TARGET_STALE->next==SelectorLabState.PICKING||next==SelectorLabState.IDLE||next==SelectorLabState.CLOSED;case ERROR->next==SelectorLabState.CLOSED;};if(!allowed)throw new IllegalStateException("Illegal Lab transition "+state+" -> "+next);state=next;}
    private static String text(Object value){return value==null?null:String.valueOf(value);}private static int number(Object value){return value instanceof Number n?n.intValue():-1;}private static long longNumber(Object value){return value instanceof Number n?n.longValue():-1;}private static String safe(String value){return value==null?"(no source declaration)":value;}
    private record PatternState(String proposalId,PatternProposal.Result proposal,FederatedPatternPreview.Result preview){}
}
