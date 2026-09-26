package io.github.testlens.selector.live;

import io.github.testlens.selector.engine.*;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.*;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.testlens.selector.engine.CandidateAnalysis.*;
import static org.junit.jupiter.api.Assertions.*;

class LiveCandidateAnalysisServiceTest {
    @Test void validatesEachDeduplicatedCandidateOnceAndNeverActs(){
        Fixture f=new Fixture();CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(By.id("save"),UsageIntent.FIND_ONE,List.of("data-qa")));
        assertEquals(result.candidates().size(),f.findElements.get());assertEquals(1,f.scripts.get());assertEquals(1,f.text.get());assertEquals(1,f.accessible.get());assertEquals(1,f.role.get());assertEquals(0,f.actions.get());
        Candidate original=result.candidates().stream().filter(Candidate::original).findFirst().orElseThrow();
        assertEquals(ValidationState.VERIFIED_IN_SCOPE,original.validation().state());assertTrue(original.origins().containsAll(List.of(Origin.ORIGINAL,Origin.ID)));
        assertEquals(Recommendation.KEEP_CURRENT,result.recommendation());
    }

    @Test void findManyAllowsMultipleAndFindOneReportsAmbiguity(){
        Fixture many=new Fixture();many.multiple=true;CandidateAnalysis a=new LiveCandidateAnalysisService().analyze(many.request(By.id("save"),UsageIntent.FIND_ONE,List.of()));
        assertEquals(ValidationState.VALID_BUT_AMBIGUOUS,a.candidates().stream().filter(Candidate::original).findFirst().orElseThrow().validation().state());
        Fixture collection=new Fixture();collection.multiple=true;CandidateAnalysis b=new LiveCandidateAnalysisService().analyze(collection.request(By.id("save"),UsageIntent.FIND_MANY,List.of()));
        assertEquals(ValidationState.VALID_FOR_INTENT,b.candidates().stream().filter(Candidate::original).findFirst().orElseThrow().validation().state());
        Fixture unknown=new Fixture();unknown.multiple=true;CandidateAnalysis c=new LiveCandidateAnalysisService().analyze(unknown.request(By.id("save"),UsageIntent.UNKNOWN,List.of()));
        assertEquals(ValidationState.VALID_FOR_INTENT,c.candidates().stream().filter(Candidate::original).findFirst().orElseThrow().validation().state());
    }

    @Test void wrongTargetAndInvalidSelectorNeverWin(){
        Fixture f=new Fixture();f.wrongTag=true;CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(By.tagName("button"),UsageIntent.FIND_ONE,List.of()));
        Candidate tag=result.candidates().stream().filter(Candidate::original).findFirst().orElseThrow();assertEquals(ValidationState.WRONG_TARGET,tag.validation().state());
        assertNotEquals(tag.candidateId(),result.candidates().get(0).candidateId());
    }

    @Test void invalidSelectorIsCandidateLocalAndSessionLossStopsFurtherCommands(){
        Fixture invalid=new Fixture();invalid.invalid=true;
        CandidateAnalysis invalidResult=new LiveCandidateAnalysisService().analyze(invalid.request(By.id("save"),UsageIntent.FIND_ONE,List.of()));
        assertTrue(invalidResult.candidates().stream().anyMatch(c->c.validation().state()==ValidationState.INVALID_SELECTOR));
        assertEquals(invalidResult.candidates().size(),invalid.findElements.get());

        Fixture lost=new Fixture();lost.sessionLost=true;
        CandidateAnalysis lostResult=new LiveCandidateAnalysisService().analyze(lost.request(By.id("save"),UsageIntent.FIND_ONE,List.of()));
        assertEquals(1,lost.findElements.get());
        assertTrue(lostResult.issues().stream().anyMatch(i->i.code().equals("SESSION_LOST")&&i.fatal()));
        assertTrue(lostResult.candidates().stream().allMatch(c->c.validation().state()==ValidationState.SESSION_LOST));
    }

    @Test void supportedWrapperEqualityUsesWrappedElementWithoutAnotherCommand(){
        Fixture f=new Fixture();f.wrapped=true;
        CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(By.id("save"),UsageIntent.FIND_ONE,List.of()));
        assertEquals(ValidationState.VERIFIED_IN_SCOPE,result.candidates().stream().filter(Candidate::original).findFirst().orElseThrow().validation().state());
        assertEquals(result.candidates().size(),f.findElements.get());
    }

    @Test void oversizedSnapshotIsReducedAndNeverIncludesFormValues(){
        Fixture f=new Fixture();f.oversized=true;
        CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(By.id("save"),UsageIntent.FIND_ONE,List.of()));
        assertTrue(result.completeness().metadataTruncated());
        assertTrue(result.candidates().stream().filter(c->c.locator()!=null).noneMatch(c->c.locator().value().contains("password-secret")));
    }

    @Test void instrumentationIsExcludedButLookalikeApplicationElementRemains(){
        Fixture f=new Fixture();f.includeInstrumentation=true;CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(By.tagName("button"),UsageIntent.FIND_ONE,List.of()));
        assertTrue(result.completeness().instrumentationNodesExcluded()>0);
        Candidate tag=result.candidates().stream().filter(Candidate::original).findFirst().orElseThrow();assertEquals(2,tag.validation().matchCount());
    }

    @Test void customOriginalStaysOpaqueAndExecutableWithoutToString(){
        Fixture f=new Fixture();By custom=new By(){@Override public List<WebElement> findElements(SearchContext context){return context.findElements(this);}@Override public String toString(){throw new AssertionError("must not call toString");}};
        CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(custom,UsageIntent.FIND_ONE,List.of()));
        Candidate opaque=result.candidates().stream().filter(Candidate::opaqueOriginal).findFirst().orElseThrow();assertNull(opaque.locator());assertEquals(ValidationState.VERIFIED_IN_SCOPE,opaque.validation().state());
    }

    @Test void futureRemotableStrategyUsesTheOriginalExecutableHandle(){
        Fixture f=new Fixture();By future=new FutureBy();
        CandidateAnalysis result=new LiveCandidateAnalysisService().analyze(f.request(future,UsageIntent.FIND_ONE,List.of()));
        Candidate original=result.candidates().stream().filter(Candidate::original).findFirst().orElseThrow();
        assertEquals("future-strategy",original.locator().strategy());
        assertEquals(ValidationState.VERIFIED_IN_SCOPE,original.validation().state());
    }

    @Test void targetRequiredUnsafeAttributesAndShadowXpathAreExplicit(){
        Fixture f=new Fixture();LiveCandidateRequest base=f.request(null,UsageIntent.FIND_ONE,List.of());
        LiveCandidateRequest missing=new LiveCandidateRequest(base.driver(),base.searchContext(),null,base.usageIntent(),null,null,false,null,null,null,null,null,null,null,null,List.of());
        assertEquals(Recommendation.TARGET_REQUIRED,new LiveCandidateAnalysisService().analyze(missing).recommendation());
        assertThrows(IllegalArgumentException.class,()->f.request(null,UsageIntent.FIND_ONE,List.of("bad attr")));
        LiveCandidateRequest shadow=new LiveCandidateRequest(base.driver(),base.searchContext(),base.target(),base.usageIntent(),null,null,true,null,null,null,null,null,null,null,null,List.of());
        assertTrue(new LiveCandidateAnalysisService().analyze(shadow).candidates().stream().filter(c->c.origins().contains(Origin.TEXT_XPATH)).allMatch(c->c.validation().state()==ValidationState.UNSUPPORTED));
    }

    private static final class Fixture {
        final AtomicInteger scripts=new AtomicInteger(),findElements=new AtomicInteger(),text=new AtomicInteger(),accessible=new AtomicInteger(),role=new AtomicInteger(),actions=new AtomicInteger();
        final WebElement target=element("target"),other=element("other"),lookalike=element("lookalike"),instrumentation=element("instrumentation");
        boolean multiple,wrongTag,includeInstrumentation,invalid,sessionLost,wrapped,oversized;
        final WebDriver driver=(WebDriver)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebDriver.class,JavascriptExecutor.class},(proxy,method,args)->{
            if(method.getName().equals("executeScript")){scripts.incrementAndGet();return snapshot();}
            return defaultValue(method.getReturnType());});
        final SearchContext context=new SearchContext(){
            @Override public List<WebElement> findElements(By by){findElements.incrementAndGet();if(sessionLost)throw new NoSuchSessionException("gone");String strategy=strategy(by),value=value(by);if(invalid&&"xpath".equals(strategy))throw new InvalidSelectorException("bad candidate");if("tag name".equals(strategy)&&"button".equals(value)&&wrongTag)return List.of(other);List<WebElement> out=new ArrayList<>();out.add(wrapped?wrapped(target):target);if(multiple)out.add(other);if(includeInstrumentation){out.add(lookalike);out.add(instrumentation);}return out;}
            @Override public WebElement findElement(By by){throw new AssertionError("findElement must not be called");}
        };
        LiveCandidateRequest request(By original,UsageIntent intent,List<String> attrs){return new LiveCandidateRequest(driver,context,target,intent,original,null,false,null,null,null,null,null,null,CompiledPolicySet.empty(),ObservationEvidence.unavailable(),attrs);}
        Map<String,Object> snapshot(){Map<String,Object> map=new LinkedHashMap<>();map.put("tagName","button");map.put("id",oversized?"x".repeat(40_000):"save");map.put("name","submit");map.put("classList",List.of("save","css-1a2b3c"));map.put("testAttributes",Map.of("data-testid","save-test","data-qa","save-qa"));map.put("ariaLabel","Save");map.put("ancestors",List.of(Map.of("depth",1,"tagName","form","id","checkout","classList",List.of("form"),"testAttributes",Map.of())));map.put("ownedNodes",includeInstrumentation?List.of(instrumentation):List.of());map.put("instrumentationNodesExcluded",0);map.put("truncated",false);map.put("value","password-secret");return map;}
        WebElement element(String id){return (WebElement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebElement.class},(proxy,method,args)->switch(method.getName()){
            case "getText"->{text.incrementAndGet();yield "Save";}case "getAccessibleName"->{accessible.incrementAndGet();yield "Save";}case "getAriaRole"->{role.incrementAndGet();yield "button";}
            case "click","submit","sendKeys","clear"->{actions.incrementAndGet();throw new AssertionError("action forbidden");}case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);case "toString"->id;default->defaultValue(method.getReturnType());});}
        WebElement wrapped(WebElement delegate){return (WebElement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{WebElement.class,WrapsElement.class},(proxy,method,args)->switch(method.getName()){case "getWrappedElement"->delegate;case "equals"->proxy==args[0];case "hashCode"->System.identityHashCode(proxy);default->defaultValue(method.getReturnType());});}
        static String strategy(By by){return by instanceof By.Remotable remotable?remotable.getRemoteParameters().using():"custom";}
        static String value(By by){return by instanceof By.Remotable remotable?String.valueOf(remotable.getRemoteParameters().value()):null;}
    }
    private static Object defaultValue(Class<?> type){if(!type.isPrimitive())return null;if(type==boolean.class)return false;if(type==char.class)return '\0';return 0;}
    private static final class FutureBy extends By implements By.Remotable {
        @Override public List<WebElement> findElements(SearchContext context){return context.findElements(this);}
        @Override public Parameters getRemoteParameters(){return new Parameters("future-strategy","future-value");}
    }
}
