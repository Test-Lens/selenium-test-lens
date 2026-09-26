package io.github.testlens.browser;

import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.ObservationEvidence;
import io.github.testlens.selector.live.LiveCandidateAnalysisService;
import io.github.testlens.selector.live.LiveCandidateRequest;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.hud.HudOptions;
import io.github.testlens.hud.HudPreset;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectorLiveCandidateIT {
    @Test void observedWrappersAndRealHudRemainCompatible(){
        WebDriver raw=BrowserTestHarness.createDriver();
        TestLens lens=null;
        try{
            open(raw,"<button id='wrapped-save' data-testid='wrapped-save'>Save</button>");
            lens=TestLens.attach(raw,TestLensOptions.builder().hud(HudOptions.builder().preset(HudPreset.DEBUG).build()).build());
            WebDriver observed=lens.observeDriver();lens.startSession("selector live wrapper contract");
            WebElement wrapped=observed.findElement(By.id("wrapped-save"));
            CandidateAnalysis analysis=analyze(observed,observed,wrapped,By.id("wrapped-save"),false,List.of());
            assertEquals(ValidationState.VERIFIED_IN_SCOPE,original(analysis).validation().state());
            assertNotNull(((JavascriptExecutor)raw).executeScript("return document.getElementById('selenium-overlay-host')"));
            lens.finishPassed();lens=null;
        }finally{if(lens!=null)lens.finishPassed();raw.quit();}
    }

    @Test void driverParentShadowFrameStaleAndInstrumentationContracts(){
        WebDriver driver=BrowserTestHarness.createDriver();
        try{
            open(driver,"""
                <form id='checkout'><button id='save' name='submit' data-testid='save-test' data-qa='save-qa' class='stable css-1a2b3c'>Save "now"</button></form>
                <button class='stable' data-test-lens-owned-ish='true'>Application lookalike</button>
                <div id='selenium-overlay-host'><button id='save' class='stable'>Lens UI</button></div>
                <div id='host'></div><iframe id='frame' srcdoc="<button id='inside'>Inside</button>"></iframe>
                <script>host.attachShadow({mode:'open'}).innerHTML='<button id="shadow-save" data-testid="shadow-test">Shadow</button>';</script>
                """);
            WebElement target=driver.findElements(By.id("save")).get(0);
            CandidateAnalysis root=analyze(driver,driver,target,By.id("save"),false,List.of("data-qa"));
            assertEquals(Recommendation.KEEP_CURRENT,root.recommendation());
            assertEquals(ValidationState.VERIFIED_IN_SCOPE,original(root).validation().state());
            assertTrue(root.completeness().instrumentationNodesExcluded()>0);
            assertTrue(root.candidates().stream().anyMatch(c->c.origins().contains(Origin.TEST_ATTRIBUTE)&&c.locator().value().contains("data-qa")));

            WebElement form=driver.findElement(By.id("checkout"));
            CandidateAnalysis parent=analyze(driver,form,target,By.cssSelector("button.stable"),false,List.of());
            assertEquals(ValidationState.VERIFIED_IN_SCOPE,original(parent).validation().state());

            SearchContext shadow=driver.findElement(By.id("host")).getShadowRoot();WebElement shadowTarget=shadow.findElement(By.id("shadow-save"));
            CandidateAnalysis shadowResult=analyze(driver,shadow,shadowTarget,By.id("shadow-save"),true,List.of());
            assertEquals(ValidationState.VERIFIED_IN_SCOPE,original(shadowResult).validation().state());
            assertTrue(shadowResult.candidates().stream().filter(c->c.origins().contains(Origin.TEXT_XPATH)).allMatch(c->c.validation().state()==ValidationState.UNSUPPORTED));

            driver.switchTo().frame(driver.findElement(By.id("frame")));WebElement inside=driver.findElement(By.id("inside"));
            assertEquals(ValidationState.VERIFIED_IN_SCOPE,original(analyze(driver,driver,inside,By.id("inside"),false,List.of())).validation().state());
            driver.switchTo().defaultContent();

            WebElement stale=driver.findElements(By.id("save")).get(0);((JavascriptExecutor)driver).executeScript("arguments[0].remove()",stale);
            CandidateAnalysis staleResult=analyze(driver,driver,stale,By.id("save"),false,List.of());
            assertEquals(Recommendation.REVIEW_REQUIRED,staleResult.recommendation());
            assertTrue(staleResult.issues().stream().anyMatch(i->i.code().equals("STALE_TARGET")));
        }finally{driver.quit();}
    }

    private static CandidateAnalysis analyze(WebDriver driver,SearchContext context,WebElement target,By original,boolean shadow,List<String> attrs){return new LiveCandidateAnalysisService().analyze(new LiveCandidateRequest(driver,context,target,UsageIntent.FIND_ONE,original,null,shadow,null,null,null,null,null,null,CompiledPolicySet.empty(),ObservationEvidence.unavailable(),attrs));}
    private static Candidate original(CandidateAnalysis analysis){return analysis.candidates().stream().filter(Candidate::original).findFirst().orElseThrow();}
    private static void open(WebDriver driver,String html){driver.get("data:text/html;charset=utf-8,"+URLEncoder.encode(html,StandardCharsets.UTF_8).replace("+","%20"));}
}
