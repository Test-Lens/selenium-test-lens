package io.github.testlens.browser;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.CandidateAnalysis;
import io.github.testlens.selector.engine.CompiledPolicySet;
import io.github.testlens.selector.engine.ObservationEvidence;
import io.github.testlens.selector.engine.SimilarityResult;
import io.github.testlens.selector.lab.SelectorLabRequest;
import io.github.testlens.selector.lab.SelectorLabResult;
import io.github.testlens.selector.lab.SelectorLabSession;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Real Chrome/Firefox contract for the caller-thread blocking Lab protocol. */
class SelectorLabBrowserIT {
    @Test void pickerDoesNotDispatchToTargetAndRestoresTimeout() {
        WebDriver driver=BrowserTestHarness.createDriver();
        try {
            open(driver,"""
                    <form id='form'><div id='parent'><button id='target' type='submit' data-testid='save'>Save</button></div></form>
                    <script>
                    window.targetClicks=0;window.parentClicks=0;window.submits=0;window.captureEvents=0;
                    document.getElementById('target').addEventListener('click',()=>window.targetClicks++);
                    document.getElementById('parent').addEventListener('click',()=>window.parentClicks++);
                    document.getElementById('form').addEventListener('submit',e=>{window.submits++;e.preventDefault();});
                    document.addEventListener('click',()=>window.captureEvents++,true);
                    window.labStage=0;
                    window.labAutomation=setInterval(()=>{
                      const host=document.getElementById('selenium-overlay-host'),root=host&&host.shadowRoot;
                      if(!root)return;
                      if(window.labStage===0){const b=[...root.querySelectorAll('button')].find(x=>x.textContent==='Pick element');if(b){window.labStage=1;b.dispatchEvent(new MouseEvent('click',{bubbles:true,composed:true}));}}
                      else if(window.labStage===1){const p=root.querySelector('#stl-selector-lab-picker');const t=document.getElementById('target');if(p&&t){const q=t.getBoundingClientRect(),o={bubbles:true,cancelable:true,composed:true,clientX:q.left+2,clientY:q.top+2};['pointerdown','mousedown','mouseup','click'].forEach(n=>p.dispatchEvent(new MouseEvent(n,o)));window.labStage=2;}}
                      else if(window.labStage===2){const h=[...root.querySelectorAll('button')].find(x=>x.textContent==='Highlight matches');if(h){h.dispatchEvent(new MouseEvent('click',{bubbles:true,composed:true}));window.labStage=3;}}
                      else if(window.labStage===3&&root.querySelector('[data-test-lens-lab-highlight]')){const c=[...root.querySelectorAll('button')].find(x=>x.textContent==='Close');if(c){clearInterval(window.labAutomation);c.dispatchEvent(new MouseEvent('click',{bubbles:true,composed:true}));}}
                    },25);
                    </script>
                    """);
            Duration prior=Duration.ofSeconds(7);
            driver.manage().timeouts().scriptTimeout(prior);
            SelectorLabResult result=new SelectorLabSession(request(driver,By.id("target"))).run();
            assertEquals(prior,driver.manage().timeouts().getScriptTimeout());
            assertTrue(result.scriptTimeoutRestored());
            assertEquals(0L,number(driver,"targetClicks"));
            assertEquals(0L,number(driver,"parentClicks"));
            assertEquals(0L,number(driver,"submits"));
            assertTrue(number(driver,"captureEvents")>0,"a pre-existing document capture listener may observe instrumentation events");
            assertNull(((JavascriptExecutor)driver).executeScript("var h=document.getElementById('selenium-overlay-host');return h&&h.shadowRoot&&h.shadowRoot.querySelector('#stl-selector-lab');"));
        } finally {driver.quit();}
    }

    @Test void nestedOpenShadowTargetUsesStandardElementSerialization() {
        WebDriver driver=BrowserTestHarness.createDriver();
        try {
            open(driver,"""
                    <div id='outer'></div><script>
                    const r1=outer.attachShadow({mode:'open'}),inner=document.createElement('div');inner.id='inner';r1.append(inner);
                    const r2=inner.attachShadow({mode:'open'}),button=document.createElement('button');button.id='shadow-target';button.textContent='Shadow save';r2.append(button);
                    window.shadowClicks=0;button.addEventListener('click',()=>window.shadowClicks++);window.labStage=0;
                    window.labAutomation=setInterval(()=>{const h=document.getElementById('selenium-overlay-host'),r=h&&h.shadowRoot;if(!r)return;
                      if(window.labStage===0){const b=[...r.querySelectorAll('button')].find(x=>x.textContent==='Pick element');if(b){window.labStage=1;b.dispatchEvent(new MouseEvent('click',{bubbles:true,composed:true}));}}
                      else if(window.labStage===1){const p=r.querySelector('#stl-selector-lab-picker'),t=outer.shadowRoot.getElementById('inner').shadowRoot.getElementById('shadow-target');if(p&&t){const q=t.getBoundingClientRect(),o={bubbles:true,cancelable:true,composed:true,clientX:q.left+2,clientY:q.top+2};p.dispatchEvent(new MouseEvent('click',o));window.labStage=2;}}
                      else if(window.labStage===2&&r.querySelector('.stl-lab-row')){const c=[...r.querySelectorAll('button')].find(x=>x.textContent==='Close');if(c){clearInterval(window.labAutomation);c.dispatchEvent(new MouseEvent('click',{bubbles:true,composed:true}));}}
                    },25);
                    </script>
                    """);
            SelectorLabResult result=new SelectorLabSession(request(driver,null)).run();
            assertFalse(result.issues().contains("INVALID_TARGET_SELECTION"));
            assertEquals(0L,number(driver,"shadowClicks"));
        } finally {driver.quit();}
    }

    private static SelectorLabRequest request(WebDriver driver,By original) {
        return new SelectorLabRequest(driver,driver,CandidateAnalysis.UsageIntent.FIND_ONE,original,"driver",null,null,null,null,null,null,
                CompiledPolicySet.empty(), ObservationEvidence.unavailable(),List.of(), RedactionPolicy.defaults(),
                SelectorLabRequest.DisplayMode.STANDARD,null,List.of(), Map.of(), SimilarityResult.QueryScope.PROJECT,false,null);
    }
    private static long number(WebDriver driver,String name){return ((Number)((JavascriptExecutor)driver).executeScript("return window[arguments[0]]||0;",name)).longValue();}
    private static void open(WebDriver driver,String html){driver.get("data:text/html;charset=utf-8,"+ URLEncoder.encode(html, StandardCharsets.UTF_8).replace("+","%20"));}
}
