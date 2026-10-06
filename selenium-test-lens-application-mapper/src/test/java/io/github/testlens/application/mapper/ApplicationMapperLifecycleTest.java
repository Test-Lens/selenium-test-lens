package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationMapperLifecycleTest {
    @Test
    void zeroCrawlDepthPerformsNoBrowserCommands() {
        AtomicInteger commands = new AtomicInteger();
        WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, arguments) -> {
                    commands.incrementAndGet();
                    return defaultValue(method.getReturnType());
                });
        ApplicationMapper mapper = ApplicationMapper.start(
                driver,
                ApplicationMapperOptions.builder("Example")
                        .mode(ApplicationMapperOptions.Mode.SAFE_EXPLORE)
                        .maxCrawlDepth(0)
                        .build());

        assertEquals(0, mapper.safeExplore());
        assertEquals(0, commands.get());
    }

    @Test
    void historyDecisionRequiresARealAdditionalEntry() {
        assertEquals(false, ApplicationMapper.createdHistoryEntry(-1, 1));
        assertEquals(false, ApplicationMapper.createdHistoryEntry(4, 4));
        assertEquals(false, ApplicationMapper.createdHistoryEntry(5, 4));
        assertEquals(true, ApplicationMapper.createdHistoryEntry(4, 5));
    }

    @Test
    void rejectedStateCannotBecomeTransitionSource() {
        ObserveFixture fixture = new ObserveFixture();
        PageScanner.DiscoveredElement button = fixture.discoveredButton();
        PageIdentityResolver probe = new PageIdentityResolver("probe", ApplicationOverrides.empty());
        String emptyFingerprint = probe.resolve(
                fixture.url, fixture.title, List.of(), fixture.testAttributes, RedactionPolicy.defaults()).observationFingerprint();
        String buttonFingerprint = probe.resolve(
                fixture.url, fixture.title, List.of(button), fixture.testAttributes, RedactionPolicy.defaults()).observationFingerprint();
        ApplicationOverrides overrides = new ApplicationOverrides(
                ApplicationOverrides.SCHEMA_VERSION,
                Map.of(), Map.of(), Set.of(), Set.of(),
                Map.of(emptyFingerprint, "single-page", buttonFingerprint, "single-page"),
                Map.of());
        ApplicationMapper mapper = ApplicationMapper.start(
                fixture.driver,
                ApplicationMapperOptions.builder("Example")
                        .maxPageStates(1)
                        .overrides(overrides)
                        .build());

        MappingObservation retained = mapper.observe();
        MappingObservation rejected = mapper.observe();

        assertNotEquals(retained.stateId(), rejected.stateId());
        assertEquals(ApplicationModel.Completeness.PARTIAL, rejected.completeness());
        assertTrue(rejected.limitations().stream().anyMatch(limit -> limit.code().equals("MAX_PAGE_STATES")));
        String rejectedElementId = rejected.elementIds().get(0);
        MappingException failure = assertThrows(
                MappingException.class,
                () -> mapper.beginTransition(rejectedElementId, ApplicationModel.Action.CLICK));
        assertEquals(MappingException.Code.PAGE_STATE_UNRETAINED, failure.code());
        assertTrue(failure.getMessage().contains("not retained"));
        assertTrue(mapper.model().transitions().isEmpty());
        assertFalse(mapper.transitionPending());
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        return 0;
    }

    private static final class ObserveFixture {
        private final String url = "https://example.test/app";
        private final String title = "Application";
        private final List<String> testAttributes = List.of("data-testid");
        private final AtomicInteger discoveries = new AtomicInteger();
        private final WebElement target = element();
        private final WebDriver driver = (WebDriver) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{WebDriver.class, JavascriptExecutor.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "getCurrentUrl" -> url;
                    case "getTitle" -> title;
                    case "getWindowHandle" -> "window-1";
                    case "executeScript" -> executeScript((String) arguments[0]);
                    case "findElements" -> List.of(target);
                    case "findElement" -> target;
                    default -> defaultValue(method.getReturnType());
                });

        private Object executeScript(String script) {
            if (script.contains("const max=arguments[0]")) {
                if (discoveries.getAndIncrement() == 0) return discovery(List.of());
                return discovery(List.of(Map.ofEntries(
                        Map.entry("element", target),
                        Map.entry("shadow", false),
                        Map.entry("tag", "button"),
                        Map.entry("type", "button"),
                        Map.entry("role", "button"),
                        Map.entry("label", "Save"),
                        Map.entry("accessibleName", "Save"),
                        Map.entry("text", "Save"),
                        Map.entry("disabled", false),
                        Map.entry("regionKey", ""),
                        Map.entry("structuralHint", "main|button|save"),
                        Map.entry("testAttributes", Map.of("data-testid", "save")))));
            }
            return selectorSnapshot();
        }

        private Map<String, Object> discovery(List<?> elements) {
            return Map.of(
                    "elements", elements,
                    "regions", List.of(),
                    "discovered", elements.size(),
                    "truncated", false,
                    "closedShadowPossible", 0,
                    "shadowRoots", 0,
                    "shadowDepthReached", 0);
        }

        private Map<String, Object> selectorSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("tagName", "button");
            snapshot.put("id", "save");
            snapshot.put("name", "save");
            snapshot.put("classList", List.of("primary"));
            snapshot.put("testAttributes", Map.of("data-testid", "save"));
            snapshot.put("ariaLabel", "Save");
            snapshot.put("ancestors", List.of());
            snapshot.put("ownedNodes", List.of());
            snapshot.put("instrumentationNodesExcluded", 0);
            snapshot.put("truncated", false);
            return snapshot;
        }

        private PageScanner.DiscoveredElement discoveredButton() {
            return new PageScanner.DiscoveredElement(
                    target, (SearchContext) driver, false, "button", "button", "button", "Save", "Save", "Save",
                    null, false, null, "main|button|save", Map.of("data-testid", "save"));
        }

        private WebElement element() {
            return (WebElement) Proxy.newProxyInstance(
                    getClass().getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, arguments) -> switch (method.getName()) {
                        case "getText", "getAccessibleName" -> "Save";
                        case "getAriaRole" -> "button";
                        case "getAttribute" -> null;
                        case "equals" -> proxy == arguments[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "save-button";
                        default -> defaultValue(method.getReturnType());
                    });
        }
    }
}
