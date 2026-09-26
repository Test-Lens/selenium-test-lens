package io.github.testlens.selector.lab;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SelectorLabSessionTest {
    @Test void runsPickerAnalysisHighlightAndCloseOnCallerThreadWithoutActions(){
        Fixture f=new Fixture();SelectorLabResult result=new SelectorLabSession(f.request()).run();
        assertEquals(SelectorLabState.CLOSED,result.state());assertTrue(result.scriptTimeoutRestored());
        assertEquals(Duration.ofSeconds(5),f.scriptTimeout);assertEquals(0,f.actions.get());assertEquals(0,f.findElement.get());
        assertTrue(f.findElements.get()>0);assertEquals(1,f.highlightCalls.get());assertEquals(Thread.currentThread().getId(),f.commandThread);
    }
    @Test void snippetEscapingIsIndependentFromCssAndXpath(){
        assertEquals("By.id(\"a\\\"b\\\\c\\n\")",JavaLocatorSnippet.render(new CandidateAnalysis.Locator("id","a\"b\\c\n")));
        assertNull(JavaLocatorSnippet.render(new CandidateAnalysis.Locator("future","x")));
    }
    @Test void browserResourceIsBoundedReadOnlyAndSafeDom(){
        String js=SelectorLabJs.INIT;assertTrue(js.contains("MAX_QUEUE=16"));assertTrue(js.contains("MAX_PAYLOAD=16384"));
        assertTrue(js.contains("stopImmediatePropagation"));assertTrue(js.contains("shadowRoot.elementFromPoint"));
        assertFalse(js.contains("localStorage"));assertFalse(js.contains("sessionStorage"));assertFalse(js.contains("eval("));assertFalse(js.contains("innerHTML"));
        assertFalse(js.contains(".click()"));assertFalse(js.contains("window.location"));
    }
    @Test void onlyPreparedJetBrainsNavigationTargetReachesTheBrowser(){
        Fixture f=new Fixture();SelectorLabRequest base=f.request();
        SelectorLabRequest unsafe=new SelectorLabRequest(base.driver(),base.searchContext(),base.usageIntent(),base.originalBy(),base.contextFingerprint(),base.declarationRef(),base.modulePath(),base.logicalPath(),base.declaringSymbol(),base.usageClass(),base.usageMethod(),base.policies(),base.evidence(),base.preferredTestAttributes(),base.redactionPolicy(),base.displayMode(),base.auditProjection(),base.similarityCatalog(),base.similarityEvidence(),base.similarityScope(),base.incompleteHistory(),"javascript:alert(1)");
        assertNull(unsafe.preparedSourceNavigationTarget());
    }

    private static final class Fixture {
        final AtomicInteger actions=new AtomicInteger(),findElements=new AtomicInteger(),findElement=new AtomicInteger(),highlightCalls=new AtomicInteger();
        final WebElement target=element();Duration scriptTimeout=Duration.ofSeconds(5);String session,generation,analysisId,candidateId;int stage;long commandThread;
        final SearchContext context=new SearchContext(){public List<WebElement>findElements(By by){findElements.incrementAndGet();return List.of(target);}public WebElement findElement(By by){findElement.incrementAndGet();throw new AssertionError();}};
        final WebDriver driver=(WebDriver)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebDriver.class,JavascriptExecutor.class},(proxy,method,args)->{
            commandThread=Thread.currentThread().getId();String name=method.getName();
            if(name.equals("manage"))return options();
            if(name.equals("executeScript")){String script=String.valueOf(args[0]);Object[]values=args.length>1?(Object[])args[1]:new Object[0];if(script.contains("lab.open")){session=String.valueOf(values[0]);generation=String.valueOf(values[1]);return true;}if(script.contains("maxDepth=3,maxTokens=8"))return snapshot();if(script.contains("renderAnalysis")){Map<?,?>model=(Map<?,?>)values[0];analysisId=String.valueOf(model.get("analysisId"));List<?>rows=(List<?>)model.get("candidates");candidateId=String.valueOf(((Map<?,?>)rows.get(0)).get("candidateId"));return true;}if(script.contains("lab.highlight")){highlightCalls.incrementAndGet();return 1;}return true;}
            if(name.equals("executeAsyncScript"))return event();
            return primitive(method.getReturnType());});
        private WebDriver.Options options(){return (WebDriver.Options)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebDriver.Options.class},(p,m,a)->m.getName().equals("timeouts")?timeouts():primitive(m.getReturnType()));}
        private WebDriver.Timeouts timeouts(){return (WebDriver.Timeouts)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebDriver.Timeouts.class},(p,m,a)->{if(m.getName().equals("getScriptTimeout"))return scriptTimeout;if(m.getName().equals("scriptTimeout")){scriptTimeout=(Duration)a[0];return p;}return p;});}
        private Map<String,Object>event(){stage++;Map<String,Object>e=new LinkedHashMap<>();e.put("protocolVersion",1);e.put("sessionRef",session);e.put("documentGeneration",generation);e.put("sequence",stage);e.put("payload",Map.of());if(stage==1){e.put("kind","COMMAND");e.put("commandType","START_PICK");}else if(stage==2){e.put("kind","TARGET_SELECTION");e.put("element",target);e.put("shadowHosts",List.of());}else if(stage==3){e.put("kind","COMMAND");e.put("commandType","HIGHLIGHT_CANDIDATE");e.put("analysisId",analysisId);e.put("candidateId",candidateId);}else{e.put("kind","COMMAND");e.put("commandType","CLOSE");}return e;}
        private Map<String,Object>snapshot(){return new LinkedHashMap<>(Map.of("tagName","button","id","save","name","submit","classList",List.of("save"),"testAttributes",Map.of("data-testid","save"),"ariaLabel","Save","ancestors",List.of(),"ownedNodes",List.of(),"instrumentationNodesExcluded",0,"truncated",false));}
        private WebElement element(){return (WebElement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebElement.class},(p,m,a)->switch(m.getName()){case"getText","getAccessibleName"->"Save";case"getAriaRole"->"button";case"click","submit","sendKeys","clear"->{actions.incrementAndGet();throw new AssertionError();}case"equals"->p==a[0];case"hashCode"->System.identityHashCode(p);default->primitive(m.getReturnType());});}
        SelectorLabRequest request(){return new SelectorLabRequest(driver,context,CandidateAnalysis.UsageIntent.FIND_ONE,By.id("save"),"ctx",null,null,null,null,null,null,CompiledPolicySet.empty(),ObservationEvidence.unavailable(),List.of(),RedactionPolicy.defaults(),SelectorLabRequest.DisplayMode.STANDARD,null,List.of(),Map.of(),SimilarityResult.QueryScope.PROJECT,false,null);}
    }
    private static Object primitive(Class<?>type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==char.class)return'\0';return 0;}
}
