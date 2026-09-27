package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityCaptureDescriptor;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityManifestToolingTest {
    @Test void deterministicRoundTripRetainsLogicalRetryWithoutSecrets() {
        Attempt fail=CompatibilityAttempts.attempt(1,ResultStatus.FAILED,
                CompatibilityAttempts.failure("TimeoutException","TIMEOUT","click",null,List.of("TIMEOUT"),
                        "Authorization: Bearer top-secret 123456",FailurePhase.TEST),null,null,null);
        Attempt pass=CompatibilityAttempts.attempt(2,ResultStatus.PASSED,null,null,null,
                new EvidenceCompleteness(EvidenceRetention.SUMMARY_ONLY,CompletenessState.PARTIAL,false,false,false,false,false,SmartClickDetail.UNKNOWN));
        CompatibilityCaptureDescriptor descriptor=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.JUNIT5,"example.LoginTest","login","login",false,null,"suite")
                .requestedHeadless(RequestedHeadlessMode.HEADLESS,RequestedHeadlessProvenance.EXPLICIT_JAVA)
                .attestEffectiveHeadless(EffectiveHeadlessState.HEADLESS,EffectiveHeadlessProvenance.MANAGED_FACTORY_ATTESTED)
                .observability(ObservabilityMode.FAST,HudPreset.STANDARD,false,false,false,"SUMMARY_ONLY","OFF")
                .lifecycle(DriverScope.PER_METHOD,Lifecycle.MANAGED_INVOCATION).attempt(fail).attempt(pass).build();
        CompatibilityRunManifest manifest=new CompatibilityManifestBuilder().build(descriptor,null,null,null);
        CompatibilityManifestJson codec=new CompatibilityManifestJson();
        byte[] first=codec.write(manifest), second=codec.write(manifest);
        assertArrayEquals(first,second);
        assertEquals(manifest,codec.read(first));
        assertEquals(2,manifest.attempts().size());
        assertTrue(manifest.terminalResult().passedAfterRetry());
        String json=new String(first,StandardCharsets.UTF_8);
        assertFalse(json.contains("top-secret"));
        assertFalse(json.contains("Authorization"));
        assertFalse(json.contains("generatedAt"));
    }

    @Test void strictCodecRejectsDuplicateUnknownSemanticAndTamperedId() {
        CompatibilityRunManifest manifest=manifest(); CompatibilityManifestJson codec=new CompatibilityManifestJson();
        String json=new String(codec.write(manifest),StandardCharsets.UTF_8);
        assertThrows(RuntimeException.class,()->codec.read("{\"schemaVersion\":1,\"schemaVersion\":1}".getBytes(StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class,()->codec.read(json.replaceFirst("\"schemaVersion\":1","\"schemaVersion\":1,\"future\":true").getBytes(StandardCharsets.UTF_8)));
        assertThrows(RuntimeException.class,()->codec.read(json.replace(manifest.manifestId(),"compatibility-manifest-v1:sha256:"+"f".repeat(64)).getBytes(StandardCharsets.UTF_8)));
    }

    @Test void writerIsPathSafeAndUsesCanonicalDefault(@TempDir Path root)throws Exception {
        CompatibilityRunManifest manifest=manifest(); CompatibilityManifestJson codec=new CompatibilityManifestJson();
        Path written=codec.writeDefault(manifest,root);
        assertTrue(written.startsWith(root.resolve("target")));
        assertEquals(manifest,codec.read(Files.readAllBytes(written)));
        assertThrows(IllegalArgumentException.class,()->codec.writeTo(manifest,root,Path.of("..","escape.json")));
        assertThrows(IllegalArgumentException.class,()->codec.writeTo(manifest,root,root.resolve("absolute.json")));
    }

    @Test void replacementFailurePreservesExistingManifestBytes(@TempDir Path root)throws Exception {
        CompatibilityRunManifest manifest=manifest();Path relative=Path.of("target","test-lens","compatibility","runs","fixed.json");
        CompatibilityManifestJson codec=new CompatibilityManifestJson();Path path=codec.writeTo(manifest,root,relative);byte[] before=Files.readAllBytes(path);
        CompatibilityManifestJson failing=new CompatibilityManifestJson((temp,destination)->{throw new java.io.IOException("injected move failure");});
        assertThrows(java.io.IOException.class,()->failing.writeTo(manifest,root,relative));
        assertArrayEquals(before,Files.readAllBytes(path));
        try(var files=Files.list(path.getParent())){assertFalse(files.anyMatch(p->p.getFileName().toString().startsWith("compatibility-manifest-")&&p.toString().endsWith(".tmp")));}
    }

    @Test void explicitSeleniumCaptureUsesOnlyAllowlistedFactsAndTwoDisplayCommands() {
        MutableCapabilities caps=new MutableCapabilities();
        caps.setCapability("browserName","chrome");caps.setCapability("browserVersion","152.0");caps.setCapability("platformName","WINDOWS");
        caps.setCapability("chrome",Map.of("chromedriverVersion","152.1","userDataDir","C:\\secret-profile"));
        caps.setCapability("goog:chromeOptions",Map.of("debuggerAddress","localhost:9222","args",List.of("--headless=new")));
        AtomicInteger windowCalls=new AtomicInteger(),scriptCalls=new AtomicInteger();
        WebDriver driver=fakeDriver(caps,windowCalls,scriptCalls);
        SeleniumCompatibilityCapture.Snapshot capture=new SeleniumCompatibilityCapture().capture(driver);
        assertEquals(1,windowCalls.get());assertEquals(1,scriptCalls.get());assertEquals(2,capture.displayCommandBudget());
        assertEquals("chrome",capture.browser().name().value());
        assertEquals("152.1",capture.browser().driverVersion().value());
        assertEquals(Knowledge.UNKNOWN,capture.capabilityEffectiveHeadless().knowledge());
        assertFalse(capture.browser().allowlistedCapabilityFacts().toString().contains("debugger"));
        assertFalse(capture.browser().allowlistedCapabilityFacts().toString().contains("secret-profile"));
        assertEquals(1200,capture.display().viewportWidth().value());
    }

    @Test void firefoxTypedHeadlessCapabilityIsAcceptedAndPrecreatedChromeRemainsUnknown() {
        MutableCapabilities firefox=new MutableCapabilities();firefox.setCapability("browserName","firefox");firefox.setCapability("browserVersion","145");firefox.setCapability("platformName","LINUX");firefox.setCapability("moz:headless",true);firefox.setCapability("moz:geckodriverVersion","0.36");
        SeleniumCompatibilityCapture.Snapshot ff=new SeleniumCompatibilityCapture().capture(fakeDriver(firefox,new AtomicInteger(),new AtomicInteger()));
        assertEquals(EffectiveHeadlessState.HEADLESS,ff.capabilityEffectiveHeadless().value());
        MutableCapabilities chrome=new MutableCapabilities();chrome.setCapability("browserName","chrome");
        SeleniumCompatibilityCapture.Snapshot external=new SeleniumCompatibilityCapture().capture(fakeDriver(chrome,new AtomicInteger(),new AtomicInteger()));
        assertEquals(Knowledge.UNKNOWN,external.capabilityEffectiveHeadless().knowledge());
    }

    @Test void conflictingTrustedAndCapabilityHeadlessDoesNotSilentlyOverride() {
        MutableCapabilities firefox=new MutableCapabilities();firefox.setCapability("browserName","firefox");firefox.setCapability("moz:headless",false);
        SeleniumCompatibilityCapture.Snapshot captured=new SeleniumCompatibilityCapture().capture(fakeDriver(firefox,new AtomicInteger(),new AtomicInteger()));
        CompatibilityCaptureDescriptor d=CompatibilityCaptureDescriptor.builder().attestEffectiveHeadless(EffectiveHeadlessState.HEADLESS,EffectiveHeadlessProvenance.TRUSTED_CAPTURE_DESCRIPTOR).build();
        CompatibilityRunManifest result=new CompatibilityManifestBuilder().build(d,captured,null,null);
        assertEquals(Knowledge.CONFLICTED,result.execution().effectiveHeadless().knowledge());
        assertTrue(result.issues().stream().anyMatch(i->i.code().equals("EFFECTIVE_HEADLESS_CONFLICT")));
    }

    @Test void requestedIntentMismatchIsRecordedWithoutPretendingIntentIsEffectiveState() {
        CompatibilityCaptureDescriptor d=CompatibilityCaptureDescriptor.builder()
                .requestedHeadless(RequestedHeadlessMode.HEADLESS,RequestedHeadlessProvenance.EXPLICIT_JAVA)
                .attestEffectiveHeadless(EffectiveHeadlessState.HEADED,EffectiveHeadlessProvenance.TRUSTED_CAPTURE_DESCRIPTOR).build();
        CompatibilityRunManifest result=new CompatibilityManifestBuilder().build(d,null,null,null);
        assertEquals(RequestedHeadlessMode.HEADLESS,result.execution().requestedHeadless().value());
        assertEquals(EffectiveHeadlessState.HEADED,result.execution().effectiveHeadless().value());
        assertTrue(result.issues().stream().anyMatch(i->i.code().equals("REQUESTED_EFFECTIVE_HEADLESS_MISMATCH")));
    }

    @Test void traceAdapterMarksFastSummaryOnlyAndNeverParsesSmartClickProse() {
        String trace="""
                {"schemaVersion":"1.0","session":{"status":"PASSED","durationMs":12,
                "retention":{"passedSessionRetention":"SUMMARY_ONLY"},"summary":{"totalArtifacts":1},
                "events":[{"type":"ACTION","status":"SUCCESS","durationMs":3,
                "attributes":{"message":"NATIVE -> ACTIONS -> POINT -> JS"}}]}}
                """;
        CompatibilityTraceAdapter.ImportResult imported=new CompatibilityTraceAdapter().read(trace.getBytes(StandardCharsets.UTF_8));
        Attempt attempt=imported.attempts().get(0);
        assertEquals(EvidenceRetention.SUMMARY_ONLY,attempt.evidence().retention());
        assertEquals(CompletenessState.PARTIAL,attempt.evidence().behavior());
        assertEquals(SmartClickDetail.UNKNOWN,attempt.behavior().interactions().smartClickDetail());
        assertTrue(imported.issues().stream().anyMatch(i->i.code().equals("TRACE_SUMMARY_ONLY")));
    }

    @Test void traceAdapterConsumesCurrentRuntimeProducerAndOmitsRawFailureAndLocator() {
        UiTestLensSession session=UiTestLensSession.start("random-session-name-is-not-identity");
        session.addEvent(TraceEvent.builder(TraceEventType.LOCATOR_RESOLVE,TraceStatus.PASSED,"find")
                .duration(Duration.ofMillis(4))
                .attribute("metadata.testlens.selector.schemaVersion","1")
                .attribute("metadata.testlens.selector.locator.strategy","id")
                .attribute("metadata.testlens.selector.locator.value","token-secret-selector")
                .attribute("metadata.testlens.selector.locator.valueState","KNOWN")
                .attribute("metadata.testlens.selector.locator.display","By.id: token-secret-selector")
                .attribute("metadata.testlens.selector.locator.supportKind","STRUCTURED")
                .attribute("metadata.testlens.selector.context.knowledge","KNOWN")
                .attribute("metadata.testlens.selector.context.segmentCount","1")
                .attribute("metadata.testlens.selector.context.segment.0.kind","DRIVER_ROOT")
                .attribute("metadata.testlens.selector.context.segment.0.knowledge","KNOWN")
                .attribute("metadata.testlens.selector.usageIntent","FIND_ONE")
                .attribute("metadata.testlens.selector.outcome","RESOLVED")
                .attribute("metadata.testlens.selector.matchCount.knowledge","KNOWN")
                .attribute("metadata.testlens.selector.matchCount.value","1").build());
        session.finishFailed(new RuntimeException("Bearer top-secret-value at port 12345"));
        byte[] produced=session.exportJson().getBytes(StandardCharsets.UTF_8);
        CompatibilityTraceAdapter.ImportResult imported=new CompatibilityTraceAdapter().read(produced);
        assertEquals(1,imported.attempts().size());
        assertEquals(ResultStatus.FAILED,imported.attempts().get(0).result());
        assertEquals(1,imported.attempts().get(0).behavior().locators().resolved());
        assertFalse(imported.toString().contains("token-secret-selector"));
        assertFalse(imported.toString().contains("top-secret-value"));
        assertFalse(imported.toString().contains("random-session-name"));
    }

    @Test void bundleProjectsAllowlistAndReportsBrowserConflict() {
        byte[] runtime="{\"browserName\":\"firefox\",\"browserVersion\":\"145\",\"platformName\":\"LINUX\",\"currentWindowHandle\":{\"status\":\"AVAILABLE\",\"value\":\"secret-handle\"},\"currentUrl\":{\"status\":\"AVAILABLE\",\"value\":\"https://secret\"},\"viewport\":{\"status\":\"AVAILABLE\",\"value\":{\"width\":800,\"height\":600}},\"windowSize\":{\"status\":\"AVAILABLE\",\"value\":{\"width\":900,\"height\":700}}}".getBytes(StandardCharsets.UTF_8);
        byte[] config="{\"observabilityMode\":\"FAST\",\"liveHud\":false,\"automaticFeedback\":false,\"sourceNavigation\":false,\"passedTraceRetention\":\"SUMMARY_ONLY\",\"networkCaptureMode\":\"NOT_INITIALIZED\"}".getBytes(StandardCharsets.UTF_8);
        FailureBundleCompatibilityAdapter.Snapshot bundle=new FailureBundleCompatibilityAdapter().read(runtime,config);
        assertEquals("firefox",bundle.browser().name().value());
        assertEquals(800,bundle.display().viewportWidth().value());assertEquals(900,bundle.display().windowWidth().value());
        assertEquals("FAST",bundle.configuration().observabilityMode().value());assertFalse(bundle.configuration().liveHud().value());
        assertFalse(bundle.toString().contains("secret-handle"));assertFalse(bundle.toString().contains("https://secret"));
        MutableCapabilities caps=new MutableCapabilities();caps.setCapability("browserName","chrome");
        SeleniumCompatibilityCapture.Snapshot direct=new SeleniumCompatibilityCapture().capture(fakeDriver(caps,new AtomicInteger(),new AtomicInteger()));
        CompatibilityRunManifest merged=new CompatibilityManifestBuilder().build(CompatibilityCaptureDescriptor.builder().build(),direct,null,bundle);
        assertEquals(Knowledge.CONFLICTED,merged.browser().name().knowledge());
        assertTrue(merged.issues().stream().anyMatch(i->i.code().equals("BROWSER_NAME_CONFLICT")));
    }

    private static CompatibilityRunManifest manifest(){
        CompatibilityCaptureDescriptor descriptor=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.TESTNG,"example.Test","method","logical",false,null,null)
                .attempt(CompatibilityAttempts.attempt(1,ResultStatus.PASSED,null,null,null,null)).build();
        return new CompatibilityManifestBuilder().build(descriptor,null,null,null);
    }

    private static WebDriver fakeDriver(Capabilities capabilities,AtomicInteger windowCalls,AtomicInteger scriptCalls){
        WebDriver.Window window=(WebDriver.Window)Proxy.newProxyInstance(WebDriver.class.getClassLoader(),new Class[]{WebDriver.Window.class},(p,m,a)->{
            if(m.getName().equals("getSize")){windowCalls.incrementAndGet();return new Dimension(1280,720);}return null;});
        WebDriver.Options options=(WebDriver.Options)Proxy.newProxyInstance(WebDriver.class.getClassLoader(),new Class[]{WebDriver.Options.class},(p,m,a)->m.getName().equals("window")?window:null);
        return (WebDriver)Proxy.newProxyInstance(WebDriver.class.getClassLoader(),new Class[]{WebDriver.class,HasCapabilities.class,JavascriptExecutor.class},(p,m,a)->switch(m.getName()){
            case "getCapabilities" -> capabilities;
            case "manage" -> options;
            case "executeScript" -> {scriptCalls.incrementAndGet();yield Map.of("width",1200L,"height",650L,"dpr",1.25);}
            case "toString" -> "fake-driver";
            default -> null;
        });
    }
}
