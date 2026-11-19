package io.github.testlens.selenium.network;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.bidi.HasBiDi;
import org.openqa.selenium.bidi.module.Network;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@EnabledIfSystemProperty(named = "bidi.browser", matches = "true")
class BrowserBinaryTest {
    @Test void localChrome() throws Exception { capture(false); }
    @Test void rawRemoteWebDriver() throws Exception {
        assumeTrue(!System.getProperty("bidi.grid", "").isBlank(), "NOT RUN: no test Grid configured");
        capture(true);
    }

    private void capture(boolean remote) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            byte[] body = "<!doctype html><title>Lens BiDi binary smoke</title>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        WebDriver driver = null;
        NetworkDiagnostics diagnostics = null;
        Network foreign = null;
        try {
            ChromeOptions options = new ChromeOptions().enableBiDi();
            options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
            String binary = System.getProperty("test.chrome.binary", "");
            if (!binary.isBlank()) options.setBinary(binary);
            driver = remote ? new RemoteWebDriver(URI.create(System.getProperty("bidi.grid")).toURL(), options) : new ChromeDriver(options);
            // Lens gets the RAW remote before any application augmentation or BiDi initialization.
            diagnostics = new NetworkDiagnostics(driver).start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
            assertTrue(diagnostics.isStarted(), diagnostics.summary().toString());
            String origin = "http://127.0.0.1:" + server.getAddress().getPort();
            driver.get(origin + "/first");
            assertEquals(NetworkWaitStatus.MATCHED, diagnostics.waitForResponse("/first", 200).status());
            NetworkEvent first = diagnostics.events().stream().filter(event -> event.response() != null && event.response().url().endsWith("/first")).findFirst().orElseThrow();
            assertNotNull(first.correlatedRequest());
            assertEquals(first.response().requestId(), first.correlatedRequest().id());

            // A separately augmented remote uses its own connection; the local case shares Lens's connection.
            WebDriver foreignView = driver instanceof HasBiDi ? driver : new Augmenter().augment(driver);
            foreign = new Network(foreignView);
            LinkedBlockingQueue<String> observed = new LinkedBlockingQueue<>();
            foreign.onResponseCompleted(event -> {
                String url = event.getRequest().getUrl();
                if (url.endsWith("/restart") || url.endsWith("/stopped")) observed.add(url);
            });
            diagnostics.start(NetworkDiagnosticsOptions.builder().captureMode(NetworkCaptureMode.BIDI).build());
            assertTrue(diagnostics.isStarted());
            driver.get(origin + "/restart");
            assertEquals(NetworkWaitStatus.MATCHED, diagnostics.waitForResponse("/restart", 200).status());
            assertEquals(origin + "/restart", observed.poll(10, TimeUnit.SECONDS));
            diagnostics.stop();
            driver.get(origin + "/stopped");
            assertEquals(origin + "/stopped", observed.poll(10, TimeUnit.SECONDS));
            assertEquals("Lens BiDi binary smoke", driver.getTitle());
        } finally {
            try {
                if (diagnostics != null) diagnostics.stop();
                if (foreign != null) foreign.close();
            } finally {
                try { if (driver != null) driver.quit(); } finally { server.stop(0); }
            }
        }
    }
}
