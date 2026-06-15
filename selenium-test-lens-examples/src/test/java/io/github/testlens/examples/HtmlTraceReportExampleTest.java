package io.github.testlens.examples;

import io.github.testlens.JsOverlayDebug;
import io.github.testlens.core.logging.InMemoryLogSink;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.export.HtmlReportTheme;
import io.github.testlens.core.trace.TraceJsonExporter;
import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.core.trace.export.TraceHtmlExporter;
import io.github.testlens.core.trace.export.TraceReportBundleExporter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HtmlTraceReportExampleTest {

    @Disabled("Documentation-only example; requires a real WebDriver.")
    @Test
    void htmlTraceReportUsage() {
        WebDriver driver = null; // replace with a real driver

        JsOverlayDebug overlay = new JsOverlayDebug(driver);
        UiTestLensSession session = overlay.startSession("Checkout flow");

        overlay.step("Save form", () -> {
            overlay.getByTestId("save-button").click();
            overlay.expect(overlay.getByTestId("toast")).toContainText("Saved");
        });

        overlay.attachScreenshot("Save form", Path.of("target/screenshots/save-form.png"));
        overlay.attachVideo("Checkout video", Path.of("target/videos/checkout-flow.mp4"));

        Path report = session.exportHtmlReport();

        assertNotNull(session);
        assertNotNull(report);
    }

    @Test
    void logOnlyHtmlReportUsage() {
        InMemoryLogSink logs = new InMemoryLogSink();
        logs.accept(UiTestLensLogEntry.info("Opening checkout"));
        logs.accept(UiTestLensLogEntry.warn("Retrying slow save button"));

        Path report = logs.exportHtmlReport();

        assertTrue(Files.exists(report));
    }

    @Test
    void suiteHtmlReportUsage() {
        UiTestLensSession checkout = UiTestLensSession.start("Checkout flow");
        checkout.finishPassed();
        UiTestLensSession profile = UiTestLensSession.start("Profile flow");
        profile.finishSkipped("Example only");

        Path report = new TraceHtmlExporter().exportSuiteToDefault(List.of(checkout, profile),
                TraceHtmlExportOptions.builder()
                        .theme(HtmlReportTheme.AUTO)
                        .build());

        assertTrue(Files.exists(report));
    }

    @Test
    void jsonAndBundleReportUsage() {
        UiTestLensSession checkout = UiTestLensSession.start("Checkout flow");
        checkout.finishPassed();
        UiTestLensSession profile = UiTestLensSession.start("Profile flow");
        profile.finishSkipped("Example only");

        Path json = new TraceJsonExporter().exportSuiteToDefault(List.of(checkout, profile));
        Path bundle = new TraceReportBundleExporter().exportSuiteToDefault(List.of(checkout, profile));

        assertTrue(Files.exists(json));
        assertTrue(Files.exists(bundle));
    }
}
