package io.github.testlens.browser;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityCaptureDescriptor;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import io.github.testlens.compatibility.tooling.CompatibilityManifestBuilder;
import io.github.testlens.compatibility.tooling.CompatibilityManifestJson;
import io.github.testlens.compatibility.tooling.SeleniumCompatibilityCapture;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import static org.junit.jupiter.api.Assertions.*;

class CompatibilityCaptureIT {
    @Test void explicitlyCapturesAllowlistedBrowserAndDisplayFacts()throws Exception {
        BrowserTestHarness.ExecutorCommandMetrics metrics=new BrowserTestHarness.ExecutorCommandMetrics();
        WebDriver driver=BrowserTestHarness.createDriver(metrics);
        try {
            metrics.reset();
            long captureStarted=System.nanoTime();
            SeleniumCompatibilityCapture.Snapshot snapshot=new SeleniumCompatibilityCapture().capture(driver);
            long captureElapsedNanos=System.nanoTime()-captureStarted;
            assertEquals(2,snapshot.displayCommandBudget());
            assertEquals(2,metrics.total(),"capture must use one window-size and one script command");
            assertEquals(BrowserTestHarness.browserName(),snapshot.browser().name().value());
            assertEquals(Knowledge.KNOWN,snapshot.browser().version().knowledge());
            assertEquals(Knowledge.KNOWN,snapshot.browser().platform().knowledge());
            assertEquals(Knowledge.KNOWN,snapshot.display().windowWidth().knowledge());
            assertEquals(Knowledge.KNOWN,snapshot.display().viewportWidth().knowledge());
            System.out.printf("COMPAT_CAPTURE_METRIC browser=%s headed=%s commands=%d elapsedMs=%.3f capabilities=%s%n",
                    BrowserTestHarness.browserName(),System.getProperty("headed","false"),metrics.total(),captureElapsedNanos/1_000_000d,
                    snapshot.browser().allowlistedCapabilityFacts());
            assertFalse(snapshot.browser().allowlistedCapabilityFacts().containsKey("debuggerAddress"));
            assertFalse(snapshot.browser().allowlistedCapabilityFacts().containsKey("userDataDir"));

            boolean headed=Boolean.parseBoolean(System.getProperty("headed","false"));
            CompatibilityCaptureDescriptor descriptor=CompatibilityCaptureDescriptor.builder()
                    .testKey(Framework.JUNIT5,getClass().getName(),"explicitlyCapturesAllowlistedBrowserAndDisplayFacts","browser-capture",false,null,"browser-contract")
                    .requestedHeadless(headed?RequestedHeadlessMode.HEADED:RequestedHeadlessMode.HEADLESS,RequestedHeadlessProvenance.CALLER_SUPPLIED)
                    .attestEffectiveHeadless(headed?EffectiveHeadlessState.HEADED:EffectiveHeadlessState.HEADLESS,EffectiveHeadlessProvenance.MANAGED_FACTORY_ATTESTED)
                    .observability(ObservabilityMode.DEFAULT,HudPreset.STANDARD,true,true,true,"RETAIN_TRACE","OFF")
                    .attempt(CompatibilityAttempts.attempt(1,ResultStatus.PASSED,null,null,null,null)).build();
            CompatibilityRunManifest manifest=new CompatibilityManifestBuilder().build(descriptor,snapshot,null,null);
            assertEquals(headed?EffectiveHeadlessState.HEADED:EffectiveHeadlessState.HEADLESS,manifest.execution().effectiveHeadless().value());
            Path root=Path.of("target","compatibility-browser-contract").toAbsolutePath().normalize();Files.createDirectories(root);
            CompatibilityManifestJson json=new CompatibilityManifestJson();Path artifact=json.writeDefault(manifest,root);
            assertEquals(manifest,json.read(Files.readAllBytes(artifact)));
            if("chrome".equals(BrowserTestHarness.browserName()))assertEquals(Knowledge.UNKNOWN,snapshot.capabilityEffectiveHeadless().knowledge(),"Chrome UA/options are not effective-headless evidence");
        } finally { driver.quit(); }
    }
}
