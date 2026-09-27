package io.github.testlens.compatibility.tooling;

import io.github.testlens.compatibility.engine.StaticCompatibilityReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;

import java.nio.file.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StaticCompatibilityAnalyzerTest {
    @TempDir Path temp;

    @Test void detectsPreciseJdkNativeConstructsAndRejectsNamesakesAndUnusedImports()throws Exception{
        Path src=Files.createDirectories(temp.resolve("src/test/java/demo"));
        Files.writeString(src.resolve("Risk.java"),"""
            package demo; import java.awt.*; import java.io.File;
            class Risk { void test() throws Exception {
              Robot r=new Robot(); r.mousePress(1); r.keyPress(2); r.createScreenCapture(new Rectangle());
              Desktop.getDesktop().open(new File("secret-token")); new FileDialog((Frame)null);
              Toolkit.getDefaultToolkit().getScreenSize();
              if(GraphicsEnvironment.isHeadless()){} if(Boolean.getBoolean("java.awt.headless")){}
            }}
            """);
        Path other=Files.createDirectories(temp.resolve("src/test/java/other"));
        Files.writeString(other.resolve("Robot.java"),"package other; class Robot { void mousePress(int x){} } class Local { void x(){new Robot().mousePress(1);} }");
        Files.writeString(src.resolve("Unused.java"),"package demo; import java.awt.Robot; class Unused { java.awt.Color c; }");
        var report=scan(temp.resolve("src/test/java"),List.of());Set<String> codes=codes(report);
        assertTrue(codes.containsAll(Set.of("NATIVE_ROBOT_INPUT","NATIVE_ROBOT_SCREEN_CAPTURE","DESKTOP_NATIVE_ACTION","NATIVE_FILE_DIALOG","TOOLKIT_SCREEN_ASSUMPTION","EXPLICIT_HEADLESS_BRANCH")),codes.toString());
        assertEquals(1,report.findings().stream().filter(f->f.code().equals("NATIVE_ROBOT_INPUT")).count());
        var nativeCoordinates=report.findings().stream().filter(f->f.code().equals("TOOLKIT_SCREEN_ASSUMPTION")).findFirst().orElseThrow();
        assertEquals("TEST_ASSUMPTION",nativeCoordinates.category().name());
        assertEquals("REVIEW",nativeCoordinates.severity().name());
        assertEquals("REVIEW_INTERACTION_ASSUMPTION",nativeCoordinates.recommendation().name());
        assertNotEquals("ALIGN_VIEWPORT_AND_RERUN",nativeCoordinates.recommendation().name());
        assertTrue(report.findings().stream().allMatch(f->f.causalState().name().equals("HYPOTHESIS")&&f.codeChangeRequired().name().equals("UNKNOWN")));
        String json=new String(new StaticCompatibilityReportJson().write(report));assertFalse(json.contains("secret-token"));assertFalse(json.contains(temp.toString()));
    }

    @Test void detectsBoundedJsWindowAndHandleEvidenceWithoutTreatingMissingEvidenceAsFact()throws Exception{
        Path src=Files.createDirectories(temp.resolve("src/test/java/demo"));
        Files.writeString(src.resolve("Web.java"),"""
            package demo; import org.openqa.selenium.*; import java.util.*;
            class Web { static final String SCREEN="return window.screen.width"; WebDriver driver;
              void x(String dynamic){ JavascriptExecutor js=(JavascriptExecutor)driver;
                js.executeScript(SCREEN); js.executeScript("return document.hasFocus()"); js.executeScript(dynamic);
                driver.manage().window().setSize(new Dimension(1200,800)); driver.manage().window().maximize();
                new ArrayList<>(driver.getWindowHandles()).get(1);
              }
              void robust(){ Set<String> before=driver.getWindowHandles(); driver.getWindowHandles().stream().filter(h->!before.contains(h)).findFirst(); }
            }
            """);
        var r=scan(temp.resolve("src/test/java"),List.of(jarOf(JavascriptExecutor.class)));Set<String> c=codes(r);
        assertTrue(c.containsAll(Set.of("JS_SCREEN_ASSUMPTION","JS_FOCUS_VISIBILITY_ASSUMPTION","EXPLICIT_WINDOW_SIZE_CONFIGURATION","WINDOW_MAXIMIZE_OR_FULLSCREEN","WINDOW_HANDLE_ORDER_ASSUMPTION")));
        assertTrue(r.issues().contains("DYNAMIC_JAVASCRIPT_UNANALYZED:1"));
        assertEquals(1,r.findings().stream().filter(f->f.code().equals("WINDOW_HANDLE_ORDER_ASSUMPTION")).count());
        var setSize=r.findings().stream().filter(f->f.code().equals("EXPLICIT_WINDOW_SIZE_CONFIGURATION")).findFirst().orElseThrow();
        assertEquals("INFO",setSize.severity().name());assertEquals("CONFIGURATION",setSize.category().name());
        String json=new String(new StaticCompatibilityReportJson().write(r));assertFalse(json.contains("window.screen.width"));
        String html=new String(new StaticCompatibilityReportHtml().write(r));assertTrue(html.contains("Source-only findings are hypotheses"));assertFalse(html.contains("Baseline"));assertFalse(html.contains("window.screen.width"));assertFalse(html.contains("http://"));assertFalse(html.contains("https://"));
    }

    @Test void toolkitMetricReadAloneRemainsAnInformationalFact()throws Exception{
        Path src=Files.createDirectories(temp.resolve("src/test/java/demo"));
        Files.writeString(src.resolve("Metrics.java"),"package demo; import java.awt.Toolkit; class Metrics { void x(){ Toolkit.getDefaultToolkit().getScreenSize(); } }");
        var report=scan(temp.resolve("src/test/java"),List.of());
        assertFalse(codes(report).contains("TOOLKIT_SCREEN_ASSUMPTION"));
        var metric=report.findings().stream().filter(f->f.code().equals("TOOLKIT_SCREEN_METRIC_READ")).findFirst().orElseThrow();
        assertEquals("INFO",metric.severity().name());
        assertEquals("COLLECT_MORE_EVIDENCE",metric.recommendation().name());
    }

    @Test void deterministicIdsIgnoreWhitespaceAndReportsAreDeterministic()throws Exception{
        Path root1=Files.createDirectories(temp.resolve("a/src")),root2=Files.createDirectories(temp.resolve("b/src"));
        Files.writeString(root1.resolve("A.java"),"import java.awt.Robot; class A{void x()throws Exception{new Robot().mousePress(1);}}");
        Files.writeString(root2.resolve("A.java"),"import java.awt.Robot; /* moved */ class A { void x() throws Exception { new Robot().mousePress(1); } }");
        var a=new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(temp.resolve("a"),List.of(Path.of("src")),List.of(),null,Map.of()));
        var b=new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(temp.resolve("b"),List.of(Path.of("src")),List.of(),null,Map.of()));
        assertEquals(a.findings().get(0).findingRef(),b.findings().get(0).findingRef());
        var codec=new StaticCompatibilityReportJson();assertArrayEquals(codec.write(a),codec.write(a));
    }

    @Test void malformedAndUnsupportedFilesAffectCoverageAndTraversalIsRejected()throws Exception{
        Path src=Files.createDirectories(temp.resolve("src"));Files.writeString(src.resolve("Bad.java"),"class {");Files.writeString(src.resolve("Thing.kt"),"class Thing");
        var r=scan(src,List.of());assertEquals(1,r.coverage().filesFailed());assertEquals(1,r.coverage().unsupportedLanguages());assertFalse(r.limitations().isEmpty());
        assertThrows(IllegalArgumentException.class,()->new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(temp,List.of(Path.of("..")),List.of(),null,Map.of())));
    }

    @Test void validWebDriverAndAwtConstructsAreNotRisksAndOptionsAreTypedFacts()throws Exception{
        Path src=Files.createDirectories(temp.resolve("src/test/java/demo"));
        Files.writeString(src.resolve("Safe.java"),"""
            package demo; import java.awt.Color; import org.openqa.selenium.*;
            class Safe { void x(WebElement input, TakesScreenshot shot, JavascriptExecutor js){
              input.sendKeys("not-persisted"); shot.getScreenshotAs(OutputType.BYTES);
              js.executeScript("return window.innerWidth"); Color c=Color.RED;
            }}
            """);
        var safe=scan(temp.resolve("src/test/java"),List.of(jarOf(JavascriptExecutor.class)));
        assertTrue(safe.findings().isEmpty(),safe.findings().toString());

        Path optionPkg=Files.createDirectories(temp.resolve("src/test/java/org/openqa/selenium/chrome"));
        Files.writeString(optionPkg.resolve("ChromeOptions.java"),"package org.openqa.selenium.chrome; public class ChromeOptions { public ChromeOptions addArguments(String s){return this;} }");
        Files.writeString(src.resolve("Options.java"),"package demo; import org.openqa.selenium.chrome.ChromeOptions; class Options { void x(){new ChromeOptions().addArguments(\"--headless=new\").addArguments(\"--user-data-dir=/secret/path\");} }");
        var options=scan(temp.resolve("src/test/java"),List.of());assertTrue(codes(options).containsAll(Set.of("EXPLICIT_HEADLESS_CONFIGURATION","PROFILE_DEPENDENCY")));
        String json=new String(new StaticCompatibilityReportJson().write(options));assertFalse(json.contains("/secret/path"));assertFalse(json.contains("user-data-dir"));
    }

    private StaticCompatibilityReport scan(Path source,List<Path> cp)throws Exception{return new StaticCompatibilityAnalyzer().analyze(new StaticCompatibilityScanRequest(temp,List.of(temp.relativize(source)),cp,null,Map.of()));}
    private static Set<String>codes(StaticCompatibilityReport r){Set<String>x=new HashSet<>();r.findings().forEach(f->x.add(f.code()));return x;}
    private static Path jarOf(Class<?> type)throws Exception{return Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI());}
}
