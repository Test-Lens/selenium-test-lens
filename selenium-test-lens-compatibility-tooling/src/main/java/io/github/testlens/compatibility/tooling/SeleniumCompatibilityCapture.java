package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.CompatibilityDigests;
import io.github.testlens.compatibility.engine.CompatibilityRunManifest.*;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Explicit, bounded two-command Selenium metadata capture. Never installed into normal runtime. */
public final class SeleniumCompatibilityCapture {
    public static final int DISPLAY_COMMAND_BUDGET = 2;

    public Snapshot capture(WebDriver driver) {
        Objects.requireNonNull(driver,"driver");
        List<Issue> issues=new java.util.ArrayList<>();
        Map<String,String> facts=new TreeMap<>();
        Fact<String> name=Fact.unknown(),version=Fact.unknown(),platform=Fact.unknown(),driverName=Fact.unknown(),driverVersion=Fact.unknown();
        Fact<EffectiveHeadlessState> effective=Fact.unknown();
        Fact<EffectiveHeadlessProvenance> effectiveProvenance=Fact.unknown();
        if(driver instanceof HasCapabilities capable) {
            var capabilities=capable.getCapabilities();
            name=text(capabilities.getBrowserName(),Provenance.CAPABILITY_REPORTED);
            version=text(capabilities.getBrowserVersion(),Provenance.CAPABILITY_REPORTED);
            platform=text(String.valueOf(capabilities.getPlatformName()),Provenance.CAPABILITY_REPORTED);
            known(facts,"browserName",name); known(facts,"browserVersion",version); known(facts,"platformName",platform);
            String lower=name.knowledge()==Knowledge.KNOWN?name.value().toLowerCase(Locale.ROOT):"";
            if(lower.contains("firefox")) {
                driverName=Fact.known("geckodriver",Provenance.CAPABILITY_REPORTED);
                driverVersion=text(capabilities.getCapability("moz:geckodriverVersion"),Provenance.CAPABILITY_REPORTED);
                Object marker=capabilities.getCapability("moz:headless");
                if(marker instanceof Boolean state) {
                    effective=Fact.known(state?EffectiveHeadlessState.HEADLESS:EffectiveHeadlessState.HEADED,Provenance.CAPABILITY_REPORTED);
                    effectiveProvenance=Fact.known(EffectiveHeadlessProvenance.CAPABILITY_REPORTED,Provenance.CAPABILITY_REPORTED);
                    facts.put("moz:headless",String.valueOf(state));
                }
            } else if(lower.contains("chrome")) {
                driverName=Fact.known("chromedriver",Provenance.CAPABILITY_REPORTED);
                Object chrome=capabilities.getCapability("chrome");
                if(chrome instanceof Map<?,?> map) driverVersion=text(map.get("chromedriverVersion"),Provenance.CAPABILITY_REPORTED);
                // Chrome exposes no approved typed effective-headless capability here. User agent is deliberately ignored.
            }
            known(facts,"driverVersion",driverVersion);
        } else issues.add(new Issue("CAPABILITIES_UNSUPPORTED",IssueSeverity.REVIEW,List.of()));

        Fact<Integer> ww=Fact.unknown(),wh=Fact.unknown(),vw=Fact.unknown(),vh=Fact.unknown(); Fact<Double>dpr=Fact.unknown();
        try { Dimension size=driver.manage().window().getSize(); ww=intFact(size.getWidth()); wh=intFact(size.getHeight()); }
        catch(RuntimeException failure){issues.add(new Issue("WINDOW_SIZE_CAPTURE_FAILED",IssueSeverity.REVIEW,List.of(failure.getClass().getName())));}
        if(driver instanceof JavascriptExecutor js) {
            try {
                Object raw=js.executeScript("return {width:window.innerWidth,height:window.innerHeight,dpr:window.devicePixelRatio};");
                if(raw instanceof Map<?,?> map){vw=intFact(map.get("width"));vh=intFact(map.get("height"));dpr=doubleFact(map.get("dpr"));}
                else issues.add(new Issue("VIEWPORT_CAPTURE_INVALID",IssueSeverity.REVIEW,List.of()));
            } catch(RuntimeException failure){issues.add(new Issue("VIEWPORT_CAPTURE_FAILED",IssueSeverity.REVIEW,List.of(failure.getClass().getName())));}
        } else issues.add(new Issue("JAVASCRIPT_CAPTURE_UNSUPPORTED",IssueSeverity.REVIEW,List.of()));
        String capabilityDigest="compatibility-capabilities-v1:sha256:"+CompatibilityDigests.digest("compatibility-capabilities-v1",facts.entrySet().stream().map(e->e.getKey()+"="+e.getValue()).toArray(String[]::new));
        Browser browser=new Browser(name,version,driverName,driverVersion,platform,facts,capabilityDigest);
        Display display=new Display(ww,wh,vw,vh,dpr);
        return new Snapshot(browser,display,effective,effectiveProvenance,List.copyOf(issues),DISPLAY_COMMAND_BUDGET);
    }

    public record Snapshot(Browser browser,Display display,Fact<EffectiveHeadlessState> capabilityEffectiveHeadless,
                           Fact<EffectiveHeadlessProvenance> capabilityEffectiveHeadlessProvenance,
                           List<Issue> issues,int displayCommandBudget) {}
    private static Fact<String> text(Object value,Provenance provenance){if(value==null)return Fact.unknown();String text=String.valueOf(value).trim();return text.isBlank()||"null".equalsIgnoreCase(text)?Fact.unknown():Fact.known(limit(text,256),provenance);}
    private static Fact<Integer> intFact(Object value){return value instanceof Number n&&n.intValue()>0?Fact.known(n.intValue(),Provenance.EXPLICIT_WEBDRIVER_CAPTURE):Fact.unknown();}
    private static Fact<Double> doubleFact(Object value){return value instanceof Number n&&n.doubleValue()>0?Fact.known(n.doubleValue(),Provenance.EXPLICIT_WEBDRIVER_CAPTURE):Fact.unknown();}
    private static void known(Map<String,String> out,String key,Fact<String> fact){if(fact.knowledge()==Knowledge.KNOWN)out.put(key,fact.value());}
    private static String limit(String value,int max){return value.codePointCount(0,value.length())<=max?value:value.substring(0,value.offsetByCodePoints(0,max));}
}
