package io.github.testlens.studio.browser;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLocalBrowserSessionProviderTest {
    @Test
    void constructionIsLazyAndPreflightDoesNotCreateDriver() {
        AtomicInteger probes = new AtomicInteger();
        AtomicInteger creates = new AtomicInteger();
        DefaultLocalBrowserSessionProvider provider = provider(BrowserAvailability.AVAILABLE, probes, creates, new AtomicInteger());

        assertEquals(0, probes.get());
        assertEquals(0, creates.get());
        assertEquals(BrowserAvailability.AVAILABLE, provider.preflight(request(Ownership.STUDIO_OWNED)));
        assertEquals(1, probes.get());
        assertEquals(0, creates.get());
    }

    @Test
    void openCreatesSessionOnlyAfterSuccessfulPreflightAndPreservesOwnership() {
        AtomicInteger probes = new AtomicInteger();
        AtomicInteger creates = new AtomicInteger();
        AtomicInteger quits = new AtomicInteger();
        DefaultLocalBrowserSessionProvider provider = provider(BrowserAvailability.AVAILABLE, probes, creates, quits);

        BrowserSession session = provider.open(request(Ownership.CALLER_OWNED));

        assertEquals(1, probes.get());
        assertEquals(1, creates.get());
        assertEquals(Ownership.CALLER_OWNED, session.ownership());
        session.close();
        assertEquals(0, quits.get());
    }

    @Test
    void unavailablePreflightPreventsDriverCreation() {
        for (BrowserAvailability availability : new BrowserAvailability[]{
                BrowserAvailability.NOT_AVAILABLE,
                BrowserAvailability.UNSUPPORTED,
                BrowserAvailability.CONFIGURATION_INVALID}) {
            AtomicInteger creates = new AtomicInteger();
            DefaultLocalBrowserSessionProvider provider = provider(availability, new AtomicInteger(), creates, new AtomicInteger());

            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> provider.open(request(Ownership.STUDIO_OWNED)));

            assertTrue(failure.getMessage().contains(availability.name()));
            assertEquals(0, creates.get());
        }
    }

    @Test
    void preflightMapsBoundedFailureClassesWithoutLaunchingDriver() {
        AtomicInteger creates = new AtomicInteger();
        DefaultLocalBrowserSessionProvider invalid = new DefaultLocalBrowserSessionProvider(
                request -> { throw new IllegalArgumentException("bad configuration"); },
                request -> { creates.incrementAndGet(); return driver(new AtomicInteger()); });
        DefaultLocalBrowserSessionProvider unsupported = new DefaultLocalBrowserSessionProvider(
                request -> { throw new UnsupportedOperationException("unsupported"); },
                request -> { creates.incrementAndGet(); return driver(new AtomicInteger()); });
        DefaultLocalBrowserSessionProvider unavailable = new DefaultLocalBrowserSessionProvider(
                request -> { throw new IllegalStateException("missing browser"); },
                request -> { creates.incrementAndGet(); return driver(new AtomicInteger()); });

        assertEquals(BrowserAvailability.CONFIGURATION_INVALID, invalid.preflight(request(Ownership.STUDIO_OWNED)));
        assertEquals(BrowserAvailability.UNSUPPORTED, unsupported.preflight(request(Ownership.STUDIO_OWNED)));
        assertEquals(BrowserAvailability.NOT_AVAILABLE, unavailable.preflight(request(Ownership.STUDIO_OWNED)));
        assertEquals(BrowserAvailability.CONFIGURATION_INVALID, unavailable.preflight(null));
        assertEquals(0, creates.get());
    }

    @Test
    void configuredSeleniumBrowserPathWinsOverDefaultChromeDiscovery() {
        BrowserRequest request = request(Ownership.STUDIO_OWNED);

        var configured = DefaultLocalBrowserSessionProvider.chromeOptions(request,
                name -> name.equals(DefaultLocalBrowserSessionProvider.SELENIUM_BROWSER_PATH)
                        ? "/toolcache/chrome-155/chrome" : null);
        var defaults = DefaultLocalBrowserSessionProvider.chromeOptions(request, ignored -> null);

        assertEquals("/toolcache/chrome-155/chrome", chromeOptions(configured).get("binary"));
        assertFalse(chromeOptions(defaults).containsKey("binary"),
                "an absent SE_BROWSER_PATH must preserve Selenium's default discovery");
        assertEquals(List.of("--headless=new"), chromeArguments(defaults),
                "sandboxing must remain enabled unless the host explicitly disables it");
    }

    @Test
    void noSandboxRequiresExplicitHostConfiguration() {
        BrowserRequest request = request(Ownership.STUDIO_OWNED);

        var configured = DefaultLocalBrowserSessionProvider.chromeOptions(request,
                name -> name.equals(DefaultLocalBrowserSessionProvider.SELENIUM_NO_SANDBOX) ? "true" : null);
        var unrelated = DefaultLocalBrowserSessionProvider.chromeOptions(request,
                name -> name.equals(DefaultLocalBrowserSessionProvider.SELENIUM_NO_SANDBOX) ? "false" : null);

        assertEquals(List.of("--headless=new", "--no-sandbox"), chromeArguments(configured));
        assertEquals(List.of("--headless=new"), chromeArguments(unrelated));
    }

    @Test
    void verboseDriverLoggingRequiresAnExplicitDiagnosticsDirectory() throws Exception {
        Path diagnostics = Files.createTempDirectory("test-lens-browser-diagnostics-");
        try {
            assertNull(DefaultLocalBrowserSessionProvider.diagnosticChromeDriverService(ignored -> null));

            var service = DefaultLocalBrowserSessionProvider.diagnosticChromeDriverService(
                    name -> name.equals(DefaultLocalBrowserSessionProvider.SELENIUM_DIAGNOSTICS_DIRECTORY)
                            ? diagnostics.toString() : null);

            assertNotNull(service);
            try (var files = Files.list(diagnostics)) {
                assertEquals(1, files.filter(path -> path.getFileName().toString().matches("chromedriver-.*\\.log")).count());
            }
        } finally {
            try (var files = Files.list(diagnostics)) {
                files.forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
            Files.deleteIfExists(diagnostics);
        }
    }

    @Test
    void certificationProfileUsesAControlledWritableRoot() throws Exception {
        Path root = Files.createTempDirectory("test-lens-browser-profiles-");
        try {
            var options = DefaultLocalBrowserSessionProvider.chromeOptions(request(Ownership.STUDIO_OWNED),
                    name -> name.equals(DefaultLocalBrowserSessionProvider.SELENIUM_PROFILE_DIRECTORY)
                            ? root.toString() : null);

            String argument = chromeArguments(options).stream()
                    .filter(value -> value.startsWith("--user-data-dir="))
                    .findFirst().orElseThrow();
            Path profile = Path.of(argument.substring("--user-data-dir=".length()));
            assertTrue(Files.isDirectory(profile));
            assertTrue(profile.toRealPath().startsWith(root.toRealPath()));
        } finally {
            try (var profiles = Files.list(root)) {
                profiles.forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                });
            }
            Files.deleteIfExists(root);
        }
    }

    private static DefaultLocalBrowserSessionProvider provider(BrowserAvailability availability,
                                                               AtomicInteger probes,
                                                               AtomicInteger creates,
                                                               AtomicInteger quits) {
        return new DefaultLocalBrowserSessionProvider(
                request -> { probes.incrementAndGet(); return availability; },
                request -> { creates.incrementAndGet(); return driver(quits); });
    }

    private static BrowserRequest request(Ownership ownership) {
        return new BrowserRequest(Purpose.MAPPING, Browser.CHROME, ownership, true);
    }

    private static WebDriver driver(AtomicInteger quits) {
        return (WebDriver) Proxy.newProxyInstance(
                DefaultLocalBrowserSessionProviderTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("quit")) quits.incrementAndGet();
                    return method.getReturnType().isPrimitive() ? primitiveDefault(method.getReturnType()) : null;
                });
    }

    private static Object primitiveDefault(Class<?> type) {
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> chromeOptions(org.openqa.selenium.chrome.ChromeOptions options) {
        return (Map<String, Object>) options.asMap().get(org.openqa.selenium.chrome.ChromeOptions.CAPABILITY);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<String> chromeArguments(org.openqa.selenium.chrome.ChromeOptions options) {
        return (java.util.List<String>) chromeOptions(options).get("args");
    }
}
