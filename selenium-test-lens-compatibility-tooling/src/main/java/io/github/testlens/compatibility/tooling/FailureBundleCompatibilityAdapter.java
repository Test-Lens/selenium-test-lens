package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityDigests;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Allowlisted projection of failure-bundle runtime/configuration JSON; URLs, handles and paths are ignored. */
public final class FailureBundleCompatibilityAdapter {
    public Snapshot read(byte[] runtimeJson,byte[] configurationJson) {
        Map<String,Object> runtime=runtimeJson==null?Map.of():StrictJson.object(runtimeJson);
        Map<String,Object> config=configurationJson==null?Map.of():StrictJson.object(configurationJson);
        Fact<String> name=text(runtime.get("browserName")),version=text(runtime.get("browserVersion")),platform=text(runtime.get("platformName"));
        Map<String,String> facts=new TreeMap<>();known(facts,"browserName",name);known(facts,"browserVersion",version);known(facts,"platformName",platform);
        String capDigest="compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1",facts.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toArray(String[]::new));
        Browser browser=new Browser(name,version,Fact.unknown(),Fact.unknown(),platform,facts,capDigest);
        Map<String,Object> window=successfulProbe(runtime.get("windowSize")); Map<String,Object> viewport=successfulProbe(runtime.get("viewport"));
        Display display=new Display(integer(window.get("width")),integer(window.get("height")),integer(viewport.get("width")),integer(viewport.get("height")),Fact.unknown());
        ConfigurationProjection configuration=new ConfigurationProjection(text(config.get("observabilityMode")),bool(config.get("liveHud")),bool(config.get("automaticFeedback")),bool(config.get("sourceNavigation")),text(config.get("passedTraceRetention")),text(config.get("networkCaptureMode")));
        String evidence="compatibility-failure-bundle-v1:sha256:"+CompatibilityDigests.digest("compatibility-failure-bundle-v1",capDigest,display.toString(),configuration.toString());
        return new Snapshot(browser,display,configuration,evidence,List.of());
    }
    public record ConfigurationProjection(Fact<String> observabilityMode,Fact<Boolean> liveHud,Fact<Boolean> automaticFeedback,Fact<Boolean> sourceNavigation,Fact<String> retention,Fact<String> networkCapture){}
    public record Snapshot(Browser browser,Display display,ConfigurationProjection configuration,String evidenceDigest,List<Issue> issues){}
    private static Map<String,Object> successfulProbe(Object value){if(value instanceof Map<?,?>m){Map<String,Object> map=cast(m);if(map.containsKey("value")&&"AVAILABLE".equals(String.valueOf(map.get("status"))))return map(map.get("value"));return map;}return Map.of();}
    private static Fact<String> text(Object v){if(v==null)return Fact.unknown();String s=String.valueOf(v).trim();return s.isBlank()||"unknown".equalsIgnoreCase(s)?Fact.unknown():Fact.known(limit(s,256),Provenance.FAILURE_BUNDLE_IMPORT);}
    private static Fact<Boolean> bool(Object v){return v instanceof Boolean b?Fact.known(b,Provenance.FAILURE_BUNDLE_IMPORT):Fact.unknown();}
    private static Fact<Integer> integer(Object v){return v instanceof Number n&&n.intValue()>0?Fact.known(n.intValue(),Provenance.FAILURE_BUNDLE_IMPORT):Fact.unknown();}
    private static void known(Map<String,String> out,String k,Fact<String> f){if(f.knowledge()==Knowledge.KNOWN)out.put(k,f.value());}
    @SuppressWarnings("unchecked")private static Map<String,Object> cast(Map<?,?>v){return(Map<String,Object>)v;}
    private static Map<String,Object> map(Object v){return v instanceof Map<?,?>m?cast(m):Map.of();}
    private static String limit(String v,int max){return v.codePointCount(0,v.length())<=max?v:v.substring(0,v.offsetByCodePoints(0,max));}
}
