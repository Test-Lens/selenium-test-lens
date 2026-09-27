package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityReportToolingTest {
    @TempDir Path temp;

    @Test void jsonAndHtmlAreDeterministicSanitizedAndStandalone() {
        var report=report("safe<test>&\"");var json=new CompatibilityReportJson();var html=new CompatibilityReportHtml();
        assertArrayEquals(json.write(report),json.write(report));assertArrayEquals(html.write(report),html.write(report));
        String j=new String(json.write(report),StandardCharsets.UTF_8),h=new String(html.write(report),StandardCharsets.UTF_8);
        assertTrue(j.contains("\"analysisMode\":\"COMPARE\""));assertTrue(j.contains("\"configurationDifferences\""));assertFalse(j.contains("sessionId"));assertFalse(j.contains("C:\\"));
        assertTrue(h.contains("&lt;test&gt;&amp;&quot;"));assertFalse(h.contains("<test>"));assertFalse(h.contains("http://"));assertFalse(h.contains("https://"));
        assertTrue(h.contains("id=\"outcome\""));assertTrue(h.contains("id=\"comparability\""));assertTrue(h.contains("id=\"severity\""));assertTrue(h.contains("id=\"category\""));assertTrue(h.contains("id=\"causal\""));
    }

    @Test void explicitManifestSetLoadIsSortedNonRecursiveAndPathSafe() throws Exception {
        Path root=temp.resolve("project");Files.createDirectories(root);Path input=root.resolve("inputs");Files.createDirectories(input);Path nested=input.resolve("nested");Files.createDirectories(nested);
        var b=manifest("one",EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1);var v=manifest("one",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1);
        CompatibilityManifestJson codec=new CompatibilityManifestJson();Files.write(input.resolve("b.json"),codec.write(b));Files.write(nested.resolve("ignored.json"),codec.write(v));
        CompatibilityComparisonTool tool=new CompatibilityComparisonTool();assertEquals(1,tool.load(List.of(input)).size());
        var compared=tool.compare(List.of(input.resolve("b.json")),List.of(write(root,"v.json",codec.write(v))),new ComparisonIntent(ComparisonIntent.Axis.HEADLESS_MODE,"headed","headless"));assertEquals(1,compared.coverage().matched());
        CompatibilityReportJson reports=new CompatibilityReportJson();Path json=reports.writeDefault(compared,root);Path html=new CompatibilityReportHtml().writeDefault(compared,root);assertTrue(Files.exists(json));assertTrue(Files.exists(html));
        assertThrows(IllegalArgumentException.class,()->reports.writeTo(compared,root,Path.of("..","escape.json")));
    }

    @Test void expectedFastEvidenceReductionDoesNotRenderAsZeroActivity(){var r=new CompatibilityCompareEngine().compare(List.of(manifest("one",EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1)),List.of(manifest("one",EffectiveHeadlessState.HEADLESS,ObservabilityMode.FAST,ResultStatus.PASSED,1)),new ComparisonIntent(ComparisonIntent.Axis.OBSERVABILITY_MODE,"standard","fast"));String json=new String(new CompatibilityReportJson().write(r),StandardCharsets.UTF_8);assertTrue(json.contains("EXPECTED_FAST_EVIDENCE_REDUCTION"));assertTrue(json.contains("evidence unavailable"));assertFalse(json.contains("zero locator activity"));}

    private CompatibilityComparisonReport report(String logical){return new CompatibilityCompareEngine().compare(List.of(manifest(logical,EffectiveHeadlessState.HEADED,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1)),List.of(manifest(logical,EffectiveHeadlessState.HEADLESS,ObservabilityMode.DEFAULT,ResultStatus.PASSED,1)),new ComparisonIntent(ComparisonIntent.Axis.HEADLESS_MODE,logical,"headless standard"));}
    private static Path write(Path root,String name,byte[]bytes)throws Exception{Path p=root.resolve(name);Files.write(p,bytes);return p;}

    static CompatibilityRunManifest manifest(String logical,EffectiveHeadlessState headless,ObservabilityMode obs,ResultStatus result,int attemptCount){
        SmartClickDetail detail=obs==ObservabilityMode.FAST?SmartClickDetail.UNKNOWN:SmartClickDetail.COMPLETE;EvidenceRetention retention=obs==ObservabilityMode.FAST?EvidenceRetention.SUMMARY_ONLY:EvidenceRetention.FULL_TRACE;
        BehaviorSummary behavior=new BehaviorSummary(0,0,List.of(),new LocatorSummary(0,0,0,0,0,0,List.of()),new WaitSummary(0,0,0,0,0),new InteractionSummary(0,0,0,detail,List.of()),new ContextSummary(0,0,0,0,0),List.of(),0,List.of(),0);
        EvidenceCompleteness evidence=new EvidenceCompleteness(retention,obs==ObservabilityMode.FAST?CompletenessState.PARTIAL:CompletenessState.COMPLETE,obs!=ObservabilityMode.FAST,obs!=ObservabilityMode.FAST,false,false,true,detail);
        CompatibilityCaptureDescriptor.Builder d=CompatibilityCaptureDescriptor.builder().testKey(Framework.TESTNG,"example.Test",logical,logical,false,logical,"suite").requestedHeadless(headless==EffectiveHeadlessState.HEADLESS?RequestedHeadlessMode.HEADLESS:RequestedHeadlessMode.HEADED,RequestedHeadlessProvenance.CALLER_SUPPLIED).attestEffectiveHeadless(headless,EffectiveHeadlessProvenance.TRUSTED_CAPTURE_DESCRIPTOR).observability(obs,HudPreset.STANDARD,obs==ObservabilityMode.DEFAULT,obs==ObservabilityMode.DEFAULT,false,retention.name(),"OFF").lifecycle(DriverScope.PER_METHOD,Lifecycle.MANAGED_INVOCATION).datasetKey("dataset").environmentKey("env").testSourceRevision("test-rev").systemUnderTestRevision("sut-rev").locale("en-US").timezone("UTC");
        for(int i=1;i<=attemptCount;i++)d.attempt(CompatibilityAttempts.attempt(i,i==attemptCount?result:ResultStatus.FAILED,null,behavior,new TimingSummary(1,10,Fact.known(10L,Provenance.DERIVED)),evidence));
        Browser browser=new Browser(Fact.known("chrome",Provenance.CAPABILITY_REPORTED),Fact.known("152",Provenance.CAPABILITY_REPORTED),Fact.unknown(),Fact.unknown(),Fact.known("WINDOWS",Provenance.CAPABILITY_REPORTED),Map.of(),"compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1","chrome"));Display display=new Display(Fact.known(1280,Provenance.EXPLICIT_WEBDRIVER_CAPTURE),Fact.known(900,Provenance.EXPLICIT_WEBDRIVER_CAPTURE),Fact.known(1280,Provenance.EXPLICIT_WEBDRIVER_CAPTURE),Fact.known(800,Provenance.EXPLICIT_WEBDRIVER_CAPTURE),Fact.known(1.0,Provenance.EXPLICIT_WEBDRIVER_CAPTURE));SeleniumCompatibilityCapture.Snapshot snapshot=new SeleniumCompatibilityCapture.Snapshot(browser,display,Fact.unknown(),Fact.unknown(),List.of(),2);return new CompatibilityManifestBuilder().build(d.build(),snapshot,null,null);
    }
}
