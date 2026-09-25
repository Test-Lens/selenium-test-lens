package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastObservabilityRuntimeTest {
    @TempDir Path output;

    @Test
    void fastGreenPathDoesNotInitializeOrCleanVisualRuntime() {
        AtomicInteger scripts = new AtomicInteger();
        TestLens lens = TestLens.attach(driver(scripts), TestLensOptions.builder()
                .outputRoot(output)
                .observabilityMode(ObservabilityMode.FAST)
                .build());

        lens.startSession("fast green");
        lens.finishPassed();

        assertEquals(0, scripts.get());
    }

    @Test
    void defaultStillInitializesHudAndExplicitFastHudWins() {
        AtomicInteger defaultScripts = new AtomicInteger();
        TestLens defaults = TestLens.attach(driver(defaultScripts), TestLensOptions.builder()
                .outputRoot(output.resolve("default")).build());
        defaults.startSession("default");
        defaults.finishPassed();
        assertTrue(defaultScripts.get() > 0);

        AtomicInteger fastScripts = new AtomicInteger();
        TestLens explicit = TestLens.attach(driver(fastScripts), TestLensOptions.builder()
                .outputRoot(output.resolve("explicit"))
                .observabilityMode(ObservabilityMode.FAST)
                .overlayConfig(OverlayConfig.builder().showHudPanel(true).build())
                .build());
        explicit.startSession("fast explicit HUD");
        explicit.finishPassed();
        assertTrue(fastScripts.get() > 0);
    }

    @Test
    void manualHighlightRemainsAvailableAndIsCleaned() {
        AtomicInteger scripts = new AtomicInteger();
        TestLens lens = TestLens.attach(driver(scripts), TestLensOptions.builder()
                .outputRoot(output)
                .observabilityMode(ObservabilityMode.FAST)
                .build());
        lens.startSession("manual highlight");

        lens.highlight(element(), "Save");
        int afterHighlight = scripts.get();
        lens.finishPassed();

        assertTrue(afterHighlight > 0);
        assertTrue(scripts.get() > afterHighlight, "a touched visual runtime must be cleaned");
    }

    @Test
    void nativeObservationStillDelegatesExactlyOnceWithoutPresentation() {
        AtomicInteger scripts = new AtomicInteger();
        AtomicInteger finds = new AtomicInteger();
        AtomicInteger clicks = new AtomicInteger();
        WebElement element = (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("click")) { clicks.incrementAndGet(); return null; }
                    if (method.getName().equals("toString")) return "native-element";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return true;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
        WebDriver raw = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().equals("findElement")) { finds.incrementAndGet(); return element; }
                    if (method.getName().startsWith("execute")) { scripts.incrementAndGet(); return null; }
                    if (method.getName().equals("toString")) return "native-fast-driver";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
        TestLens lens = TestLens.attach(raw, TestLensOptions.builder()
                .outputRoot(output.resolve("native"))
                .observabilityMode(ObservabilityMode.FAST)
                .build());
        lens.startSession("native FAST");

        lens.observeDriver().findElement(By.id("save")).click();
        lens.finishPassed();

        assertEquals(1, finds.get());
        assertEquals(1, clicks.get());
        assertEquals(0, scripts.get());
    }

    private static WebDriver driver(AtomicInteger scripts) {
        return (WebDriver) Proxy.newProxyInstance(FastObservabilityRuntimeTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("execute")) {
                        scripts.incrementAndGet();
                        return Boolean.TRUE;
                    }
                    if (method.getName().equals("toString")) return "fast-driver";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return false;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
    }

    private static WebElement element() {
        return (WebElement) Proxy.newProxyInstance(FastObservabilityRuntimeTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("toString")) return "element";
                    Class<?> type = method.getReturnType();
                    if (type == boolean.class) return true;
                    if (type.isPrimitive()) return 0;
                    return null;
                });
    }
}
