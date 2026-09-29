package io.github.testlens.browser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Deterministic, opt-in frame capture for the public 0.4.0 documentation demo. */
class DocumentationMediaCaptureIT {
    private static final Duration FRAME_INTERVAL = Duration.ofMillis(400);

    @Test
    @EnabledIfSystemProperty(named = "testLens.captureDocsMedia", matches = "true")
    void capturesDocumentationScenarios() throws Exception {
        String baseUrl = System.getProperty("testLens.docsDemoUrl",
                "http://127.0.0.1:8765/selenium-test-lens/demo/hud/");
        Path output = Path.of("target", "docs-media-frames").toAbsolutePath().normalize();
        Map<String, Integer> scenarios = new LinkedHashMap<>();
        scenarios.put("hud-action-assertion-lifecycle", 27);
        scenarios.put("hud-highlight-lifecycle", 19);
        scenarios.put("hud-default-vs-fast", 18);
        scenarios.put("native-selenium-observation", 16);
        scenarios.put("smart-click-fallback", 19);

        WebDriver driver = BrowserTestHarness.createDriver();
        try {
            driver.manage().window().setSize(new Dimension(1024, 640));
            for (Map.Entry<String, Integer> scenario : scenarios.entrySet()) {
                String queryName = switch (scenario.getKey()) {
                    case "hud-action-assertion-lifecycle" -> "lifecycle";
                    case "hud-highlight-lifecycle" -> "highlights";
                    case "hud-default-vs-fast" -> "observability";
                    case "native-selenium-observation" -> "native";
                    case "smart-click-fallback" -> "smart-click";
                    default -> throw new IllegalStateException("Unexpected media scenario");
                };
                Path scenarioDirectory = output.resolve(scenario.getKey());
                Files.createDirectories(scenarioDirectory);
                try (var existingFrames = Files.list(scenarioDirectory)) {
                    for (Path frame : existingFrames.filter(path -> path.getFileName().toString().endsWith(".png")).toList()) {
                        Files.delete(frame);
                    }
                }
                String preset = "smart-click".equals(queryName) ? "&preset=DEBUG" : "";
                driver.get(baseUrl + "?scenario=" + queryName + preset);
                Thread.sleep(300);
                for (int frame = 0; frame < scenario.getValue(); frame++) {
                    byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    Files.write(scenarioDirectory.resolve(String.format("%03d.png", frame)), png);
                    Thread.sleep(FRAME_INTERVAL.toMillis());
                }
            }
        } finally {
            driver.quit();
        }
    }
}
