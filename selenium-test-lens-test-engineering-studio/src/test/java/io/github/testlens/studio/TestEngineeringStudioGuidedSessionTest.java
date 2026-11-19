package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.studio.browser.BrowserAvailability;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSession;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import io.github.testlens.studio.browser.Ownership;
import io.github.testlens.studio.projection.StudioProjections.Capability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TestEngineeringStudioGuidedSessionTest {
    @TempDir Path root;

    @Test void guidedMappingRetainsOneStudioOwnedSessionUntilServiceClose() throws Exception {
        DriverFixture fixture = new DriverFixture(Set.of());
        TestEngineeringStudioService service = studioOwnedService(fixture);

        service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
        service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
        service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);

        assertEquals(1, fixture.opens.get());
        assertEquals(3, fixture.observations.get());
        assertEquals(0, fixture.quits.get());
        service.close();
        assertEquals(1, fixture.quits.get());
    }

    @Test void firstGuidedObservationFailureDiscardsSessionAndAllowsFreshResume() throws Exception {
        DriverFixture fixture = new DriverFixture(Set.of(1));
        try (TestEngineeringStudioService service = studioOwnedService(fixture)) {
            assertThrows(IllegalStateException.class,
                    () -> service.mapApplication(ApplicationMapperOptions.Mode.GUIDED));
            assertEquals(1, fixture.quits.get());

            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);

            assertEquals(2, fixture.opens.get());
            assertEquals(2, fixture.observations.get());
            assertEquals(1, fixture.quits.get());
        }
        assertEquals(2, fixture.quits.get());
    }

    @Test void laterGuidedObservationFailureDiscardsRetainedSessionAndAllowsFreshResume() throws Exception {
        DriverFixture fixture = new DriverFixture(Set.of(2));
        try (TestEngineeringStudioService service = studioOwnedService(fixture)) {
            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
            assertThrows(IllegalStateException.class,
                    () -> service.mapApplication(ApplicationMapperOptions.Mode.GUIDED));
            assertEquals(1, fixture.quits.get());

            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);

            assertEquals(2, fixture.opens.get());
            assertEquals(3, fixture.observations.get());
            assertEquals(1, fixture.quits.get());
        }
        assertEquals(2, fixture.quits.get());
    }

    @Test void callerOwnedGuidedSessionIsNeverQuitByStudio() throws Exception {
        DriverFixture fixture = new DriverFixture(Set.of());
        TestEngineeringStudioService.Configuration configuration = configuration();
        try (TestEngineeringStudioService service = new TestEngineeringStudioService(
                configuration, null, fixture::newDriver, root.resolve("workspace"))) {
            available(service);
            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
            assertEquals(1, fixture.opens.get());
        }
        assertEquals(0, fixture.quits.get());
    }

    @Test void currentPageUsesItsOwnSessionWithoutClosingRetainedGuidedSession() throws Exception {
        DriverFixture fixture = new DriverFixture(Set.of());
        try (TestEngineeringStudioService service = studioOwnedService(fixture)) {
            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);
            service.mapApplication(ApplicationMapperOptions.Mode.CURRENT_PAGE);
            assertEquals(1, fixture.quits.get(), "only the CURRENT_PAGE session is short-lived");

            service.mapApplication(ApplicationMapperOptions.Mode.GUIDED);

            assertEquals(2, fixture.opens.get());
            assertEquals(3, fixture.observations.get());
            assertEquals(1, fixture.quits.get());
        }
        assertEquals(2, fixture.quits.get());
    }

    @Test void closeIsIdempotentAndOperationsAfterCloseFailBeforeOpeningBrowser() {
        DriverFixture fixture = new DriverFixture(Set.of());
        TestEngineeringStudioService service = studioOwnedService(fixture);

        service.close();
        service.close();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> service.mapApplication(ApplicationMapperOptions.Mode.GUIDED));
        assertTrue(failure.getMessage().toLowerCase().contains("closed"));
        assertEquals(0, fixture.opens.get());
        assertEquals(0, fixture.quits.get());
    }

    private TestEngineeringStudioService studioOwnedService(DriverFixture fixture) {
        BrowserSessionProvider provider = new BrowserSessionProvider() {
            @Override public BrowserAvailability preflight(BrowserRequest request) {
                return BrowserAvailability.AVAILABLE;
            }

            @Override public BrowserSession open(BrowserRequest request) {
                return new BrowserSession(fixture.newDriver(), Ownership.STUDIO_OWNED);
            }
        };
        TestEngineeringStudioService service = new TestEngineeringStudioService(
                configuration(), null, provider, root.resolve("workspace"));
        available(service);
        return service;
    }

    private TestEngineeringStudioService.Configuration configuration() {
        return new TestEngineeringStudioService.Configuration(
                root, List.of(), List.of(), List.of("src/test/java"), "fixture");
    }

    private static void available(TestEngineeringStudioService service) {
        service.attachCapabilities(new Capability("AVAILABLE", "test"),
                new Capability("NOT_AVAILABLE", "test"), new Capability("AVAILABLE", "test"));
    }

    private static final class DriverFixture {
        private final Set<Integer> failingObservations;
        private final AtomicInteger opens = new AtomicInteger();
        private final AtomicInteger observations = new AtomicInteger();
        private final AtomicInteger quits = new AtomicInteger();

        private DriverFixture(Set<Integer> failingObservations) {
            this.failingObservations = Set.copyOf(failingObservations);
        }

        private WebDriver newDriver() {
            opens.incrementAndGet();
            AtomicBoolean quit = new AtomicBoolean();
            return (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{WebDriver.class, JavascriptExecutor.class}, (proxy, method, arguments) -> {
                        if (method.getName().equals("quit")) {
                            if (quit.compareAndSet(false, true)) quits.incrementAndGet();
                            return null;
                        }
                        if (quit.get()) throw new IllegalStateException("driver is closed");
                        return switch (method.getName()) {
                            case "getCurrentUrl" -> "https://example.test/app";
                            case "getTitle" -> "Fixture";
                            case "getWindowHandle" -> "window-1";
                            case "executeScript" -> observation((String) arguments[0]);
                            default -> defaultValue(method.getReturnType());
                        };
                    });
        }

        private Object observation(String script) {
            if (!script.contains("const max=arguments[0]")) return Map.of();
            int number = observations.incrementAndGet();
            if (failingObservations.contains(number)) throw new IllegalStateException("controlled observe failure");
            return Map.of("elements", List.of(), "regions", List.of(), "discovered", 0,
                    "truncated", false, "closedShadowPossible", 0, "shadowRoots", 0,
                    "shadowDepthReached", 0);
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == char.class) return '\0';
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0F;
            if (type == double.class) return 0D;
            return null;
        }
    }
}
