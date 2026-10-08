package io.github.testlens.studio;

import io.github.testlens.application.mapper.ApplicationMapperOptions;
import io.github.testlens.studio.browser.BrowserAvailability;
import io.github.testlens.studio.browser.BrowserRequest;
import io.github.testlens.studio.browser.BrowserSession;
import io.github.testlens.studio.browser.BrowserSessionProvider;
import io.github.testlens.studio.projection.StudioProjections.Capability;
import io.github.testlens.studio.transport.TestEngineeringStudioServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestEngineeringStudioMutationLockTest {
    @TempDir Path root;

    @Test
    void completedScanPublishesStateAfterReleasingMutationLock() throws Exception {
        try (var service = service(null)) {
            var scan = service.scanProject();

            assertFalse(service.operationRunning());
            assertEquals(scan.source(), service.projectOverview().source());
            assertTrue(service.projectOverview().stage().actions().stream()
                    .anyMatch(action -> action.id().equals("map-application") && action.enabled()));
        }
    }

    @Test
    void genuinelyOverlappingMutationRemainsRejected() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BrowserSessionProvider blockingProvider = blockingProvider(entered, release);

        try (var service = service(blockingProvider)) {
            service.attachCapabilities(new Capability("AVAILABLE", "test"),
                    new Capability("NOT_AVAILABLE", "test"), new Capability("AVAILABLE", "test"));
            CompletableFuture<Void> mapping = CompletableFuture.runAsync(() -> {
                assertThrows(IllegalStateException.class,
                        () -> service.mapApplication(ApplicationMapperOptions.Mode.CURRENT_PAGE));
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS), "mapping did not acquire the mutation lock");

            IllegalStateException conflict = assertThrows(IllegalStateException.class, service::scanProject);
            assertEquals("A Studio operation is already running", conflict.getMessage());
            assertTrue(service.operationRunning());

            release.countDown();
            mapping.get(5, TimeUnit.SECONDS);
            assertFalse(service.operationRunning());
            assertDoesNotThrow(service::scanProject, "the next mutation must work after cleanup");
        } finally {
            release.countDown();
        }
    }

    @Test
    void overlappingHttpMutationKeepsOperationInProgressContract() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        BrowserSessionProvider blockingProvider = blockingProvider(entered, release);

        try (var service = service(blockingProvider);
             var server = new TestEngineeringStudioServer(service)) {
            service.attachCapabilities(new Capability("AVAILABLE", "test"),
                    new Capability("NOT_AVAILABLE", "test"), new Capability("AVAILABLE", "test"));
            server.start();
            HttpClient client = HttpClient.newHttpClient();
            URI actions = server.uri().resolve("api/actions");
            String origin = server.uri().toString().replaceAll("/$", "");
            CompletableFuture<HttpResponse<String>> mapping = client.sendAsync(actionRequest(
                    actions, origin, server.sessionToken(),
                    "{\"action\":\"MAP_APPLICATION\",\"mode\":\"CURRENT_PAGE\"}"),
                    HttpResponse.BodyHandlers.ofString());
            assertTrue(entered.await(5, TimeUnit.SECONDS), "mapping did not acquire the HTTP mutation lock");

            HttpResponse<String> conflict = client.send(actionRequest(actions, origin, server.sessionToken(),
                    "{\"action\":\"SCAN_PROJECT\"}"), HttpResponse.BodyHandlers.ofString());
            assertEquals(409, conflict.statusCode());
            assertTrue(conflict.body().contains("OPERATION_IN_PROGRESS"));

            release.countDown();
            assertEquals(409, mapping.get(5, TimeUnit.SECONDS).statusCode());
        } finally {
            release.countDown();
        }
    }

    private static HttpRequest actionRequest(URI actions, String origin, String token, String json) {
        return HttpRequest.newBuilder(actions)
                .header(TestEngineeringStudioServer.TOKEN_HEADER, token)
                .header("Origin", origin)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private static BrowserSessionProvider blockingProvider(CountDownLatch entered, CountDownLatch release) {
        return new BrowserSessionProvider() {
            @Override public BrowserAvailability preflight(BrowserRequest request) {
                return BrowserAvailability.AVAILABLE;
            }

            @Override public BrowserSession open(BrowserRequest request) {
                entered.countDown();
                try {
                    if (!release.await(Duration.ofSeconds(5).toMillis(), TimeUnit.MILLISECONDS)) {
                        throw new IllegalStateException("Test browser provider timed out");
                    }
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Test browser provider interrupted", failure);
                }
                throw new IllegalStateException("Test browser provider released");
            }
        };
    }

    private TestEngineeringStudioService service(BrowserSessionProvider browserProvider) {
        var configuration = new TestEngineeringStudioService.Configuration(
                root, List.of(), List.of(), List.of("src/test/java"), "fixture");
        return new TestEngineeringStudioService(configuration, null, browserProvider,
                root.resolve("workspace"));
    }
}
