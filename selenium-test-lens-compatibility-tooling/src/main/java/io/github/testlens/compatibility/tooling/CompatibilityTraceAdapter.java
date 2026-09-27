package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityDigests;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Projects current sanitized Test Lens trace JSON into bounded manifest attempt facts. */
public final class CompatibilityTraceAdapter {
    public ImportResult read(byte[] bytes) {
        Map<String,Object> root=StrictJson.object(bytes);
        if(!"1.0".equals(string(root.get("schemaVersion"))))throw new StrictJson.FormatException("Unsupported trace schemaVersion");
        List<Map<String,Object>> sessions=new ArrayList<>();
        if(root.get("session") instanceof Map<?,?> map)sessions.add(cast(map));
        if(root.get("sessions") instanceof List<?> list)for(Object value:list)if(value instanceof Map<?,?>map)sessions.add(cast(map));
        if(sessions.isEmpty())throw new StrictJson.FormatException("Trace contains no sessions");
        if(sessions.size()>64)throw new StrictJson.FormatException("Trace attempts exceed 64");
        List<Attempt> attempts=new ArrayList<>(); List<Issue> issues=new ArrayList<>(); int ordinal=1;
        for(Map<String,Object> session:sessions)attempts.add(attempt(session,ordinal++,issues));
        String digest="compatibility-trace-evidence-v1:sha256:"+CompatibilityDigests.digest("compatibility-trace-evidence-v1",java.util.HexFormat.of().formatHex(MessageDigestHolder.sha256(bytes)));
        return new ImportResult(List.copyOf(attempts),List.copyOf(issues),digest);
    }

    private Attempt attempt(Map<String,Object> session,int ordinal,List<Issue> issues){
        ResultStatus result=result(string(session.get("status")));
        Map<String,Object> retention=map(session.get("retention"));
        String retained=string(retention.get("passedSessionRetention"));
        EvidenceRetention evidenceRetention="SUMMARY_ONLY".equals(retained)?EvidenceRetention.SUMMARY_ONLY:EvidenceRetention.FULL_TRACE;
        List<Object> events=list(session.get("events"));
        if(events.size()>100_000)throw new StrictJson.FormatException("Trace event bound exceeded");
        boolean summaryOnly=evidenceRetention==EvidenceRetention.SUMMARY_ONLY;
        long retries=0,retryMs=0,waitObservations=0,waitAttempts=0,timeouts=0,waitFailures=0,waitMs=0;
        long locObs=0,resolved=0,notFound=0,errors=0,cardinality=0,locNanos=0;
        long interactions=0,recoveries=0,fallbacks=0,frames=0,shadows=0,newWindows=0,windowSwitches=0,contextFailures=0,uploads=0,artifacts=number(map(session.get("summary")).get("totalArtifacts"));
        Map<String,Long> retryReasons=new TreeMap<>(),locOutcomes=new TreeMap<>(),fallbackTypes=new TreeMap<>(),warnings=new TreeMap<>(),auth=new TreeMap<>();
        FailureSignature failure=null; long samples=0,totalDuration=0;
        for(Object raw:events){if(!(raw instanceof Map<?,?>m))continue;Map<String,Object>event=cast(m);String type=string(event.get("type"));String status=string(event.get("status"));long duration=number(event.get("durationMs"));samples++;totalDuration+=duration;
            Map<String,Object> attributes=map(event.get("attributes")); String ui=string(attributes.get("uiEventType")); String action=string(attributes.get("action"));
            if("RETRY".equals(type)){retries++;retryMs+=duration;increment(retryReasons,string(attributes.get("retry.exceptionType")));}
            if("WARNING".equals(status))increment(warnings,ui.isBlank()?type:ui);
            if(type.startsWith("ACTION")||"LOCATOR_ACTION".equals(type)){interactions++;if(bool(attributes.get("metadata.fallback"))){fallbacks++;increment(fallbackTypes,string(attributes.get("metadata.fallbackType")));}if("recovery".equals(string(attributes.get("metadata.retryKind"))))recoveries++;}
            if("NETWORK_WAIT".equals(type)||"WAIT".equals(ui)){waitObservations++;waitAttempts+=number(attributes.get("metadata.attempts"));waitMs+=number(attributes.get("metadata.elapsedMs"));if("FAILED".equals(status)){waitFailures++;if("TIMEOUT".equals(string(attributes.get("metadata.reason"))))timeouts++;}}
            if(action.contains("context.frame")||action.contains("switchTo.frame"))frames++; if(action.contains("newWindow"))newWindows++; if(action.contains("context.window")||action.contains("switchTo.window"))windowSwitches++; if((action.contains("context.")||action.contains("switchTo."))&&"FAILED".equals(status))contextFailures++; if("upload".equals(action))uploads++;
            if(ui.startsWith("AUTH_STATE_"))increment(auth,ui);
            Map<String,Object> locator=map(event.get("locatorObservation"));if(!locator.isEmpty()){locObs++;String outcome=string(locator.get("outcome"));increment(locOutcomes,outcome);if("RESOLVED".equals(outcome))resolved++;else if("NOT_FOUND".equals(outcome))notFound++;else if("ERROR".equals(outcome))errors++;locNanos+=number(locator.get("resolutionDurationNanos"));Map<String,Object>count=map(locator.get("matchCount"));if("KNOWN".equals(string(count.get("knowledge")))&&number(count.get("value"))>1)cardinality++;Map<String,Object>context=map(locator.get("context"));for(Object segment:list(context.get("segments")))if(segment instanceof Map<?,?>sm&&"SHADOW_ROOT".equals(string(cast(sm).get("kind"))))shadows++;}
            if(failure==null&&event.get("failure") instanceof Map<?,?>fm){Map<String,Object>f=cast(fm);failure=CompatibilityAttempts.failure(safeClass(string(f.get("exceptionType"))),"RUNTIME_EXCEPTION",safeCode(action),"",List.of(),string(f.get("message")),FailurePhase.UNKNOWN);}
        }
        BehaviorSummary behavior=new BehaviorSummary(retries,retryMs,groups(retryReasons),new LocatorSummary(locObs,resolved,notFound,errors,cardinality,locNanos,groups(locOutcomes)),new WaitSummary(waitObservations,waitAttempts,timeouts,waitFailures,waitMs),new InteractionSummary(interactions,recoveries,fallbacks,SmartClickDetail.UNKNOWN,groups(fallbackTypes)),new ContextSummary(frames,shadows,newWindows,windowSwitches,contextFailures),groups(auth),uploads,groups(warnings),artifacts);
        Fact<Long> logicalDuration=session.get("durationMs") instanceof Number n?Fact.known(n.longValue(),Provenance.TRACE_REPORT_IMPORT):Fact.unknown();
        EvidenceCompleteness completeness=new EvidenceCompleteness(evidenceRetention,summaryOnly?CompletenessState.PARTIAL:CompletenessState.COMPLETE,!summaryOnly&&locObs>0,!summaryOnly,reportsScreenshot(session),false,false,SmartClickDetail.UNKNOWN);
        if(summaryOnly)issues.add(new Issue("TRACE_SUMMARY_ONLY",IssueSeverity.INFO,List.of(Integer.toString(ordinal))));
        return CompatibilityAttempts.attempt(ordinal,result,failure==null?CompatibilityRunManifest.noFailure():failure,behavior,new TimingSummary(samples,totalDuration,logicalDuration),completeness);
    }
    private static boolean reportsScreenshot(Map<String,Object>s){return number(map(s.get("summary")).get("screenshots"))>0;}
    private static List<GroupCount> groups(Map<String,Long> source){return source.entrySet().stream().limit(128).map(e->new GroupCount(e.getKey(),e.getValue())).toList();}
    private static void increment(Map<String,Long> map,String key){key=safeCode(key);if(!key.isBlank()&&(map.containsKey(key)||map.size()<128))map.merge(key,1L,Long::sum);}
    private static String safeCode(String value){if(value==null||value.isBlank())return "";String s=value.trim();return s.length()<=128&&s.matches("[A-Za-z0-9_.:/@#{}+ -]+")?s:"UNSAFE_IMPORTED_CODE";}
    private static String safeClass(String value){if(value==null)return "";String s=value.trim();return s.length()<=512&&s.matches("[A-Za-z_$][A-Za-z0-9_.$]*")?s:"";}
    private static ResultStatus result(String v){try{return ResultStatus.valueOf(v);}catch(Exception ignored){return ResultStatus.UNKNOWN;}}
    @SuppressWarnings("unchecked") private static Map<String,Object> cast(Map<?,?>v){return (Map<String,Object>)v;}
    private static Map<String,Object> map(Object v){return v instanceof Map<?,?>m?cast(m):Map.of();}
    @SuppressWarnings("unchecked") private static List<Object> list(Object v){return v instanceof List<?>l?(List<Object>)l:List.of();}
    private static String string(Object v){return v==null?"":String.valueOf(v);}
    private static long number(Object v){return v instanceof Number n?Math.max(0,n.longValue()):0;}
    private static boolean bool(Object v){return Boolean.TRUE.equals(v)||"true".equalsIgnoreCase(string(v));}
    public record ImportResult(List<Attempt> attempts,List<Issue> issues,String evidenceDigest){}

    private static final class MessageDigestHolder {
        static byte[] sha256(byte[] bytes){try{return java.security.MessageDigest.getInstance("SHA-256").digest(bytes);}catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    }
}
