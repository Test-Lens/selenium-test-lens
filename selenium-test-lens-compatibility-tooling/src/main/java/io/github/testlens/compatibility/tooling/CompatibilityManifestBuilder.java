package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityCaptureDescriptor;
import io.github.testlens.compatibility.engine.CompatibilityDigests;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import io.github.testlens.compatibility.engine.CompatibilityManifestIdentity;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** One-pass deterministic orchestration of trusted, direct and imported manifest facts. */
public final class CompatibilityManifestBuilder {
    public CompatibilityRunManifest build(CompatibilityCaptureDescriptor descriptor,
                                          SeleniumCompatibilityCapture.Snapshot selenium,
                                          CompatibilityTraceAdapter.ImportResult trace,
                                          FailureBundleCompatibilityAdapter.Snapshot bundle) {
        Objects.requireNonNull(descriptor,"descriptor"); List<Issue> issues=new ArrayList<>(descriptor.issues()); List<String> evidence=new ArrayList<>();
        if(selenium!=null)issues.addAll(selenium.issues()); if(trace!=null){issues.addAll(trace.issues());evidence.add(trace.evidenceDigest());}if(bundle!=null){issues.addAll(bundle.issues());evidence.add(bundle.evidenceDigest());}
        Execution execution=mergeExecution(descriptor.execution(),selenium,bundle,issues);
        Browser browser=mergeBrowser(selenium==null?null:selenium.browser(),bundle==null?null:bundle.browser(),issues);
        Display display=mergeDisplay(selenium==null?null:selenium.display(),bundle==null?null:bundle.display(),issues);
        Configuration configuration=mergeConfiguration(descriptor.configuration(),bundle==null?null:bundle.configuration(),issues);
        List<Attempt> attempts=!descriptor.attempts().isEmpty()?descriptor.attempts():trace==null?List.of(unknownAttempt()):trace.attempts();
        attempts=attempts.stream().sorted(Comparator.comparingInt(Attempt::ordinal)).toList();
        ResultStatus terminalStatus=attempts.get(attempts.size()-1).result(); boolean retried=attempts.size()>1; boolean priorFailure=attempts.subList(0,attempts.size()-1).stream().anyMatch(a->a.result()==ResultStatus.FAILED);
        TerminalResult terminal=new TerminalResult(terminalStatus,attempts.size(),retried&&priorFailure&&terminalStatus==ResultStatus.PASSED,retried&&terminalStatus==ResultStatus.FAILED);
        Completeness completeness=new Completeness(state(descriptor.testIdentity()),executionState(execution),browserState(browser),displayState(display),contextState(descriptor.context()),behaviorState(attempts),failureState(attempts));
        issues=issues.stream().distinct().sorted(Comparator.comparing(Issue::severity).reversed().thenComparing(Issue::code)).toList(); evidence=evidence.stream().distinct().sorted().toList();
        String placeholder="compatibility-manifest-v1:sha256:"+"0".repeat(64);
        CompatibilityRunManifest provisional=new CompatibilityRunManifest(1,1,placeholder,descriptor.testIdentity(),execution,browser,display,descriptor.context(),configuration,attempts,terminal,completeness,issues,evidence);
        return new CompatibilityRunManifest(1,1,CompatibilityManifestIdentity.id(provisional),descriptor.testIdentity(),execution,browser,display,descriptor.context(),configuration,attempts,terminal,completeness,issues,evidence);
    }

    private Execution mergeExecution(Execution base,SeleniumCompatibilityCapture.Snapshot direct,FailureBundleCompatibilityAdapter.Snapshot bundle,List<Issue>issues){
        Fact<EffectiveHeadlessState> effective=base.effectiveHeadless();Fact<EffectiveHeadlessProvenance> provenance=base.effectiveHeadlessProvenance();
        if(direct!=null){Fact<EffectiveHeadlessState> candidate=direct.capabilityEffectiveHeadless();if(conflicts(effective,candidate)){effective=Fact.conflicted();provenance=Fact.conflicted();issues.add(issue("EFFECTIVE_HEADLESS_CONFLICT"));}else if(effective.knowledge()!=Knowledge.KNOWN&&candidate.knowledge()==Knowledge.KNOWN){effective=candidate;provenance=direct.capabilityEffectiveHeadlessProvenance();}}
        Fact<ObservabilityMode> mode=base.observabilityMode();Fact<HudPreset>hud=base.hudPreset();
        if(bundle!=null){Fact<ObservabilityMode> from=parse(bundle.configuration().observabilityMode(),ObservabilityMode.class);if(conflicts(mode,from)){mode=Fact.conflicted();issues.add(issue("OBSERVABILITY_CONFLICT"));}else if(mode.knowledge()!=Knowledge.KNOWN&&from.knowledge()==Knowledge.KNOWN)mode=from;}
        if(base.requestedHeadless().knowledge()==Knowledge.KNOWN&&effective.knowledge()==Knowledge.KNOWN
                &&base.requestedHeadless().value()!=RequestedHeadlessMode.UNSET
                &&!base.requestedHeadless().value().name().equals(effective.value().name()))issues.add(issue("REQUESTED_EFFECTIVE_HEADLESS_MISMATCH"));
        return new Execution(base.requestedHeadless(),base.requestedHeadlessProvenance(),effective,provenance,mode,hud,base.driverScope(),base.lifecycle());
    }
    private Browser mergeBrowser(Browser direct,Browser bundle,List<Issue>issues){if(direct==null&&bundle==null)return emptyBrowser();if(direct==null)return bundle;if(bundle==null)return direct;
        Fact<String>name=merge(direct.name(),bundle.name(),"BROWSER_NAME_CONFLICT",issues),version=merge(direct.version(),bundle.version(),"BROWSER_VERSION_CONFLICT",issues),platform=merge(direct.platform(),bundle.platform(),"BROWSER_PLATFORM_CONFLICT",issues);
        Fact<String>driverName=direct.driverName(),driverVersion=direct.driverVersion();Map<String,String>facts=new TreeMap<>();if(name.knowledge()==Knowledge.KNOWN)facts.put("browserName",name.value());if(version.knowledge()==Knowledge.KNOWN)facts.put("browserVersion",version.value());if(platform.knowledge()==Knowledge.KNOWN)facts.put("platformName",platform.value());if(driverVersion.knowledge()==Knowledge.KNOWN)facts.put("driverVersion",driverVersion.value());
        String digest="compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1",facts.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toArray(String[]::new));return new Browser(name,version,driverName,driverVersion,platform,facts,digest);}
    private Display mergeDisplay(Display direct,Display bundle,List<Issue>issues){if(direct==null&&bundle==null)return new Display(Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown());if(direct==null)return bundle;if(bundle==null)return direct;return new Display(merge(direct.windowWidth(),bundle.windowWidth(),"WINDOW_WIDTH_CONFLICT",issues),merge(direct.windowHeight(),bundle.windowHeight(),"WINDOW_HEIGHT_CONFLICT",issues),merge(direct.viewportWidth(),bundle.viewportWidth(),"VIEWPORT_WIDTH_CONFLICT",issues),merge(direct.viewportHeight(),bundle.viewportHeight(),"VIEWPORT_HEIGHT_CONFLICT",issues),direct.devicePixelRatio());}
    private Configuration mergeConfiguration(Configuration base,FailureBundleCompatibilityAdapter.ConfigurationProjection from,List<Issue>issues){Fact<Boolean>hud=base.liveHud(),feedback=base.automaticFeedback(),source=base.sourceNavigation();Fact<String>retention=base.passedTraceRetention(),network=base.networkCaptureState();if(from!=null){hud=merge(hud,from.liveHud(),"LIVE_HUD_CONFLICT",issues);feedback=merge(feedback,from.automaticFeedback(),"AUTOMATIC_FEEDBACK_CONFLICT",issues);source=merge(source,from.sourceNavigation(),"SOURCE_NAVIGATION_CONFLICT",issues);retention=merge(retention,from.retention(),"RETENTION_CONFLICT",issues);network=merge(network,from.networkCapture(),"NETWORK_CAPTURE_CONFLICT",issues);}String digest="compatibility-config-v1:sha256:"+CompatibilityDigests.digest("compatibility-config-v1",hud.toString(),feedback.toString(),source.toString(),retention.toString(),network.toString());return new Configuration(hud,feedback,source,retention,network,digest);}
    private static <T> Fact<T> merge(Fact<T>a,Fact<T>b,String code,List<Issue>issues){if(conflicts(a,b)){issues.add(issue(code));return Fact.conflicted();}return a.knowledge()==Knowledge.KNOWN?a:b;}
    private static boolean conflicts(Fact<?>a,Fact<?>b){return a!=null&&b!=null&&a.knowledge()==Knowledge.KNOWN&&b.knowledge()==Knowledge.KNOWN&&!Objects.equals(a.value(),b.value());}
    private static <E extends Enum<E>> Fact<E> parse(Fact<String>source,Class<E>type){if(source==null||source.knowledge()!=Knowledge.KNOWN)return Fact.unknown();try{return Fact.known(Enum.valueOf(type,source.value()),source.provenance());}catch(Exception ignored){return Fact.unknown();}}
    private static Attempt unknownAttempt(){return CompatibilityAttempts.attempt(1,ResultStatus.UNKNOWN,CompatibilityRunManifest.noFailure(),CompatibilityRunManifest.emptyBehavior(),new TimingSummary(0,0,Fact.unknown()),CompatibilityRunManifest.noEvidence());}
    private static Browser emptyBrowser(){String digest="compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1");return new Browser(Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),Fact.unknown(),Map.of(),digest);}
    private static Issue issue(String code){return new Issue(code,IssueSeverity.WARNING,List.of());}
    private static CompletenessState state(Fact<?>f){return f.knowledge()==Knowledge.KNOWN?CompletenessState.COMPLETE:f.knowledge()==Knowledge.CONFLICTED?CompletenessState.PARTIAL:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState executionState(Execution e){return e.effectiveHeadless().knowledge()==Knowledge.KNOWN&&e.observabilityMode().knowledge()==Knowledge.KNOWN?CompletenessState.COMPLETE:e.effectiveHeadless().knowledge()==Knowledge.CONFLICTED?CompletenessState.PARTIAL:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState browserState(Browser b){return b.name().knowledge()==Knowledge.KNOWN&&b.version().knowledge()==Knowledge.KNOWN?b.driverVersion().knowledge()==Knowledge.KNOWN?CompletenessState.COMPLETE:CompletenessState.PARTIAL:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState displayState(Display d){return d.viewportWidth().knowledge()==Knowledge.KNOWN&&d.viewportHeight().knowledge()==Knowledge.KNOWN?CompletenessState.COMPLETE:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState contextState(Context c){long known=List.of(c.datasetDigest(),c.environmentDigest(),c.testSourceRevision(),c.systemUnderTestRevision()).stream().filter(f->f.knowledge()==Knowledge.KNOWN).count();return known==4?CompletenessState.COMPLETE:known>0?CompletenessState.PARTIAL:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState behaviorState(List<Attempt>a){return a.stream().allMatch(x->x.evidence().behavior()==CompletenessState.COMPLETE)?CompletenessState.COMPLETE:a.stream().anyMatch(x->x.evidence().behavior()!=CompletenessState.NOT_AVAILABLE)?CompletenessState.PARTIAL:CompletenessState.NOT_AVAILABLE;}
    private static CompletenessState failureState(List<Attempt>a){return a.stream().anyMatch(x->x.result()==ResultStatus.FAILED&&x.failure().exceptionClass().knowledge()!=Knowledge.KNOWN)?CompletenessState.PARTIAL:CompletenessState.COMPLETE;}
}
