package docs;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.OverlayConfig;
import io.github.testlens.TestLens;
import io.github.testlens.TestLensOptions;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.react.ReactSafeExecutor;
import io.github.testlens.react.ReactSupport;
import io.github.testlens.selenium.assertions.UiAssertionOptions;
import io.github.testlens.selenium.auth.AuthRestoreOptions;
import io.github.testlens.selenium.auth.AuthState;
import io.github.testlens.selenium.auth.AuthStateManager;
import io.github.testlens.selenium.auth.AuthStateOptions;
import io.github.testlens.selenium.locator.UiLocator;
import io.github.testlens.selenium.locator.UiLocatorOptions;
import io.github.testlens.selenium.network.NetworkCaptureMode;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkDiagnosticsOptions;
import io.github.testlens.selenium.network.NetworkEvent;
import io.github.testlens.selenium.network.NetworkRequest;
import io.github.testlens.selenium.network.NetworkResponse;
import io.github.testlens.selenium.steps.UiStepOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Compilation fixture for representative stable-documentation snippets. */
public final class StableDocumentationExamples {
    private StableDocumentationExamples() {}

    public static void facadeLocatorsActionsAssertionsAndConfiguration(WebDriver driver) {
        UiLocatorOptions locatorOptions = UiLocatorOptions.builder()
                .timeout(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .maxRetries(2)
                .build();
        TestLens lens = TestLens.attach(driver, TestLensOptions.builder()
                .overlayConfig(OverlayConfig.builder().showHudPanel(true).build())
                .locatorOptions(locatorOptions)
                .outputRoot(Path.of("target", "ui-test-lens"))
                .build());
        lens.startSession("stable example");
        lens.locator(By.id("email"), "Email").fill("person@example.test");
        lens.getByRole("button", "Save").click();
        lens.step("Save profile", UiStepOptions.builder()
                .captureScreenshotOnFailure(true)
                .build(), () -> lens.getByRole("button", "Save").click());
        UiLocator status = lens.getByTextContaining("Saved");
        status.waitUntilVisible().expect(UiAssertionOptions.builder()
                .timeout(Duration.ofSeconds(2))
                .build()).toBeVisible();
        lens.captureScreenshot("after-save");
        lens.finishPassed();
    }

    public static void waitsNetworkAndReact(WebDriver driver) {
        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        overlay.waitForPageReady(Duration.ofSeconds(5));
        overlay.waitForInteractiveOrComplete(Duration.ofSeconds(5));
        overlay.attachVideoFile("checkout recording", Path.of("target", "videos", "checkout.mp4"));
        overlay.attachVideoUrl("CI recording", "https://ci.example.test/artifacts/checkout.mp4");

        NetworkDiagnostics network = overlay.network().start(NetworkDiagnosticsOptions.builder()
                .captureMode(NetworkCaptureMode.MANUAL)
                .build());
        network.addManualEvent(NetworkEvent.request(new NetworkRequest(
                "request-1", "GET", "/api/orders", "fetch", Instant.now(), Map.of())));
        network.addManualEvent(NetworkEvent.response(NetworkResponse.of(
                "request-1", "/api/orders", 200)));
        network.waitForResponse("/api/orders", 200);
        network.assertNoFailedRequests();
        network.stop();

        ReactSafeExecutor react = ReactSupport.reactSafe(overlay);
        react.click(By.cssSelector("[data-testid='save']"), "Save");
    }

    public static void browserContexts(WebDriver driver) {
        TestLens lens = TestLens.attach(driver);
        lens.switchToFrame(By.id("payment-frame"), "Payment")
                .switchToDefaultContent();
        String originalWindow = lens.currentWindowHandle();
        lens.switchToWindow(originalWindow);
    }

    public static void directTraceExport() {
        UiTestLensSession session = UiTestLensSession.start("diagnostic snapshot");
        session.finishSkipped("Environment unavailable");
        session.exportJson(Path.of("target", "snapshot.json"));
        session.exportHtml(Path.of("target", "snapshot.html"));
    }

    public static void authenticationState(WebDriver driver) {
        AuthStateManager auth = new AuthStateManager(driver);
        AuthState state = auth.captureState(AuthStateOptions.builder()
                .label("signed-in user")
                .role("buyer")
                .origin("https://app.example.test")
                .build());
        state.save(Path.of("target", "auth", "buyer.json"));
        auth.restoreState(state, AuthRestoreOptions.defaults());
    }
}
