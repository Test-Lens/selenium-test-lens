package io.github.testlens;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublicApiBoundaryTest {
    @Test
    void jsOverlayDebugExposesOnlySupportedConstructors() {
        Set<String> signatures = Arrays.stream(JsOverlayDebug.class.getConstructors())
                .map(PublicApiBoundaryTest::signature)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "JsOverlayDebug(WebDriver)",
                "JsOverlayDebug(WebDriver,OverlayConfig)"
        ), signatures);
    }

    @Test
    void implementationPlumbingIsNotBinaryPublic() throws ClassNotFoundException {
        for (String className : Set.of(
                "io.github.testlens.api.ApiOverlayContext",
                "io.github.testlens.api.ApiOverlayPlan",
                "io.github.testlens.api.ApiOverlayRule",
                "io.github.testlens.core.ScriptExecutor",
                "io.github.testlens.core.browser.OverlayBrowserScriptExecutors",
                "io.github.testlens.selenium.assertions.UiAssertionReporter",
                "io.github.testlens.selenium.business.BusinessAssertionReporter",
                "io.github.testlens.selenium.locator.UiLocatorFailureReason",
                "io.github.testlens.selenium.locator.UiLocatorResolver",
                "io.github.testlens.selenium.locator.UiLocatorResult",
                "io.github.testlens.selenium.locator.UiLocatorResult$Builder",
                "io.github.testlens.selenium.locator.UiLocatorStatus",
                "io.github.testlens.selenium.steps.UiStepContext",
                "io.github.testlens.selenium.steps.UiStepReporter"
        )) {
            assertFalse(Modifier.isPublic(Class.forName(className).getModifiers()), className);
        }
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("io.github.testlens.api.ApiCallActions"));
    }

    private static String signature(Constructor<?> constructor) {
        return constructor.getDeclaringClass().getSimpleName() + "(" +
                Arrays.stream(constructor.getParameterTypes())
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(",")) + ")";
    }
}
