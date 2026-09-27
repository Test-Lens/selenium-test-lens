package io.github.testlens.browser;

import io.github.testlens.compatibility.engine.CompatibilityAttempts;
import io.github.testlens.compatibility.engine.CompatibilityCaptureDescriptor;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest;
import io.github.testlens.compatibility.engine.CompatibilityCompareEngine;
import io.github.testlens.compatibility.engine.CompatibilityComparisonReport;
import io.github.testlens.compatibility.engine.ComparisonIntent;
import io.github.testlens.compatibility.tooling.CompatibilityManifestBuilder;
import io.github.testlens.compatibility.tooling.CompatibilityManifestJson;
import io.github.testlens.compatibility.tooling.SeleniumCompatibilityCapture;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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

    @Test void realCaptureSupportsCleanAxisAndRejectsResponsiveViewportConfounder() {
        WebDriver driver=BrowserTestHarness.createDriver();
        try {
            driver.get("data:text/html,<style>%23mobile{display:none}@media(max-width:700px){%23desktop{display:none}%23mobile{display:block}}</style><nav id=desktop>desktop</nav><button id=mobile>mobile</button>");
            driver.manage().window().setSize(new Dimension(1280,900));
            SeleniumCompatibilityCapture.Snapshot large=new SeleniumCompatibilityCapture().capture(driver);
            assertEquals("desktop",((JavascriptExecutor)driver).executeScript("return getComputedStyle(document.getElementById('desktop')).display==='none'?'mobile':'desktop'"));
            driver.manage().window().setSize(new Dimension(600,900));
            SeleniumCompatibilityCapture.Snapshot small=new SeleniumCompatibilityCapture().capture(driver);
            assertEquals("mobile",((JavascriptExecutor)driver).executeScript("return getComputedStyle(document.getElementById('desktop')).display==='none'?'mobile':'desktop'"));

            CompatibilityRunManifest headedLarge=manifest(EffectiveHeadlessState.HEADED,large);
            CompatibilityRunManifest headlessLarge=manifest(EffectiveHeadlessState.HEADLESS,large);
            CompatibilityRunManifest headlessSmall=manifest(EffectiveHeadlessState.HEADLESS,small);
            CompatibilityCompareEngine compare=new CompatibilityCompareEngine();
            CompatibilityComparisonReport clean=compare.compare(List.of(headedLarge),List.of(headlessLarge),new ComparisonIntent(ComparisonIntent.Axis.HEADLESS_MODE,"headed","headless"));
            assertNotEquals(CompatibilityComparisonReport.Comparability.NOT_COMPARABLE,clean.testComparisons().get(0).comparability().status());
            CompatibilityComparisonReport responsive=compare.compare(List.of(headedLarge),List.of(headlessSmall),new ComparisonIntent(ComparisonIntent.Axis.HEADLESS_MODE,"headed-large","headless-small"));
            assertEquals(CompatibilityComparisonReport.Comparability.NOT_COMPARABLE,responsive.testComparisons().get(0).comparability().status());
            assertTrue(responsive.testComparisons().get(0).recommendationCodes().contains(CompatibilityComparisonReport.RecommendationCode.ALIGN_VIEWPORT_AND_RERUN));
        } finally { driver.quit(); }
    }

    private CompatibilityRunManifest manifest(EffectiveHeadlessState state,SeleniumCompatibilityCapture.Snapshot snapshot){
        CompatibilityCaptureDescriptor descriptor=CompatibilityCaptureDescriptor.builder()
                .testKey(Framework.JUNIT5,getClass().getName(),"responsiveNavigation","responsive",false,"one","browser-contract")
                .requestedHeadless(state==EffectiveHeadlessState.HEADLESS?RequestedHeadlessMode.HEADLESS:RequestedHeadlessMode.HEADED,RequestedHeadlessProvenance.CALLER_SUPPLIED)
                .attestEffectiveHeadless(state,EffectiveHeadlessProvenance.MANAGED_FACTORY_ATTESTED)
                .observability(ObservabilityMode.DEFAULT,HudPreset.STANDARD,true,true,true,"FULL_TRACE","OFF")
                .lifecycle(DriverScope.PER_METHOD,Lifecycle.MANAGED_INVOCATION)
                .datasetKey("responsive-data").environmentKey("local-browser-fixture")
                .testSourceRevision("fixture-v1").systemUnderTestRevision("fixture-v1")
                .locale("en-US").timezone("UTC")
                .attempt(CompatibilityAttempts.attempt(1,ResultStatus.PASSED,null,null,null,
                        new EvidenceCompleteness(EvidenceRetention.FULL_TRACE,CompletenessState.COMPLETE,true,true,false,false,true,SmartClickDetail.UNKNOWN))).build();
        SeleniumCompatibilityCapture.Snapshot neutralAxisCapture=new SeleniumCompatibilityCapture.Snapshot(
                snapshot.browser(),snapshot.display(),Fact.unknown(),Fact.unknown(),snapshot.issues(),snapshot.displayCommandBudget());
        return new CompatibilityManifestBuilder().build(descriptor,neutralAxisCapture,null,null);
    }
}
