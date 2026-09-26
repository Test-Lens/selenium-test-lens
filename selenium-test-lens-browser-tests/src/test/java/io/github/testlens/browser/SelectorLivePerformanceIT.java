package io.github.testlens.browser;

import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.ObservationEvidence;
import io.github.testlens.selector.live.LiveCandidateAnalysisService;
import io.github.testlens.selector.live.LiveCandidateRequest;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static io.github.testlens.selector.engine.CandidateAnalysis.UsageIntent.FIND_ONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Opt-in command and latency harness; enable with {@code -Dperf.selectorLive=true}. */
class SelectorLivePerformanceIT {
    @Test void measuresMetadataGenerationRankingAndBoundedValidationSets(){
        Assumptions.assumeTrue(Boolean.getBoolean("perf.selectorLive"),"opt-in selector-live performance harness");
        WebDriver driver=BrowserTestHarness.createDriver();
        try{
            StringBuilder attributes=new StringBuilder();
            for(int i=0;i<50;i++)attributes.append(" data-perf-").append(i).append("='target'");
            open(driver,"<main id='scope'><button id='target' data-testid='target-test' class='stable c1 c2 c3 c4 c5 c6 c7'"+attributes+">Target</button></main>");
            WebElement target=driver.findElement(By.id("target"));
            long engineStart=System.nanoTime();
            CandidateAnalysis analysis=new LiveCandidateAnalysisService().analyze(new LiveCandidateRequest(driver,driver,target,FIND_ONE,By.id("target"),null,false,null,null,null,null,null,null,CompiledPolicySet.empty(),ObservationEvidence.unavailable(),List.of()));
            long engineNanos=System.nanoTime()-engineStart;
            int resultSize=analysis.candidates().stream().mapToInt(c->c.candidateId().length()+(c.locator()==null?0:c.locator().strategy().length()+c.locator().value().length())).sum();
            for(int count:List.of(5,10,25,50)){
                long started=System.nanoTime();int commands=0;
                for(int i=0;i<count;i++){commands++;assertEquals(target,driver.findElements(By.cssSelector("[data-perf-"+i+"='target']")).get(0));}
                long elapsed=System.nanoTime()-started;
                assertEquals(count,commands);
                System.out.printf("selector-live browser=%s requestedCandidates=%d metadataCommands=1 optionalMetadataCalls=3 generated=%d deduplicated=%d validationCalls=%d totalMs=%.3f engineAnalysisMs=%.3f resultChars=%d%n",
                        BrowserTestHarness.browserName(),count,analysis.completeness().generatedBeforeDedup(),analysis.completeness().candidatesAfterDedup(),commands,elapsed/1_000_000d,engineNanos/1_000_000d,resultSize);
            }
        }finally{driver.quit();}
    }

    private static void open(WebDriver driver,String html){driver.get("data:text/html;charset=utf-8,"+URLEncoder.encode(html,StandardCharsets.UTF_8).replace("+","%20"));}
}
