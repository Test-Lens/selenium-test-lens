package io.github.testlens.browser;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceArtifactType;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceFailure;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.trace.export.HtmlReportTheme;
import io.github.testlens.core.trace.export.TraceHtmlExportOptions;
import io.github.testlens.core.trace.export.TraceHtmlExporter;
import io.github.testlens.core.trace.export.TraceReportBundleExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Generates browser fixtures with the public report API, including intentionally difficult content. */
public final class HtmlReportFixtureFactory {
    private static final String LONG_TOKEN = "0123456789abcdef".repeat(18);
    private static final String LONG_URL = "https://ci.example.test/build/" + LONG_TOKEN + "?selector=%5Bdata-test%3Dcheckout%5D";

    private HtmlReportFixtureFactory() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Expected output directory");
        }
        generate(Path.of(args[0]));
    }

    static FixturePaths generate(Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Path artifacts = outputDirectory.resolve("artifacts-source");
        Files.createDirectories(artifacts);
        Path screenshot = artifacts.resolve("czytelny-zrzut.png");
        Files.write(screenshot, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="));

        UiTestLensSession passed = session("PASS — płatność <bezpieczna>", false, screenshot);
        UiTestLensSession failed = session("FAIL — zamówienie & płatność", true, screenshot);
        UiTestLensSession empty = UiTestLensSession.start("Sesja bez zdarzeń biznesowych");
        empty.attachArtifact(TraceArtifact.url("Odnośnik artefaktu", TraceArtifactType.CUSTOM_URL, LONG_URL));
        empty.finishPassed();

        TraceHtmlExporter exporter = new TraceHtmlExporter();
        Path passLight = write(exporter, passed, outputDirectory.resolve("pass-light.html"), HtmlReportTheme.LIGHT);
        Path passDark = write(exporter, passed, outputDirectory.resolve("pass-dark.html"), HtmlReportTheme.DARK);
        Path passAuto = write(exporter, passed, outputDirectory.resolve("pass-auto.html"), HtmlReportTheme.AUTO);
        Path failLight = write(exporter, failed, outputDirectory.resolve("fail-light.html"), HtmlReportTheme.LIGHT);
        Path failDark = write(exporter, failed, outputDirectory.resolve("fail-dark.html"), HtmlReportTheme.DARK);
        Path failAuto = write(exporter, failed, outputDirectory.resolve("fail-auto.html"), HtmlReportTheme.AUTO);
        Path suiteAuto = outputDirectory.resolve("suite-auto.html");
        Path suiteLight = outputDirectory.resolve("suite-light.html");
        Path suiteDark = outputDirectory.resolve("suite-dark.html");
        exporter.exportSuiteTo(List.of(passed, failed, empty), suiteAuto, options(HtmlReportTheme.AUTO));
        exporter.exportSuiteTo(List.of(passed, failed, empty), suiteLight, options(HtmlReportTheme.LIGHT));
        exporter.exportSuiteTo(List.of(passed, failed, empty), suiteDark, options(HtmlReportTheme.DARK));
        exporter.exportTo(passed, outputDirectory.resolve("pass-no-attributes.html"), TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.LIGHT).includeAttributes(false).build());
        exporter.exportTo(passed, outputDirectory.resolve("pass-compact.html"), TraceHtmlExportOptions.builder()
                .theme(HtmlReportTheme.LIGHT).compactTimeline(true).build());
        Path bundle = outputDirectory.resolve("report-bundle.zip");
        new TraceReportBundleExporter().exportSuiteTo(List.of(passed, failed, empty), bundle);
        return new FixturePaths(passLight, passDark, passAuto, failLight, failDark, failAuto,
                suiteLight, suiteDark, suiteAuto, bundle);
    }

    private static Path write(TraceHtmlExporter exporter,
                              UiTestLensSession session,
                              Path path,
                              HtmlReportTheme theme) {
        return exporter.exportTo(session, path, options(theme));
    }

    private static TraceHtmlExportOptions options(HtmlReportTheme theme) {
        return TraceHtmlExportOptions.builder()
                .title("Raport czytelności — HTML <offline>")
                .theme(theme)
                .includeAttributes(true)
                .includeStackTraces(true)
                .includeJsonPayload(true)
                .build();
    }

    private static UiTestLensSession session(String name, boolean failing, Path screenshot) {
        UiTestLensSession session = UiTestLensSession.start(name);
        TraceEventType[] types = TraceEventType.values();
        Instant origin = Instant.parse("2026-07-15T21:59:58Z");
        for (int index = 0; index < 180; index++) {
            TraceStatus status = index % 29 == 0 ? TraceStatus.WARNING : TraceStatus.PASSED;
            session.addEvent(TraceEvent.builder(types[index % types.length], status,
                            "Zdarzenie " + index + " — bardzo długa nazwa kroku sprawdzającego płatność")
                    .id("fixture-event-" + index)
                    .timestamp(origin.plusMillis(index * 137L))
                    .duration(Duration.ofMillis(index * 3L + 1))
                    .parentId(index == 0 ? "" : "fixture-parent-" + LONG_TOKEN.substring(0, 96))
                    .message("Zażółć gęślą jaźń; <tag> & wartość " + index + " — " + LONG_TOKEN)
                    .attribute("bardzo.długi.klucz.atrybutu.który.nie.może.zabrać.całej.szerokości." + index,
                            "Wartość do skopiowania: " + LONG_TOKEN)
                    .attribute("selector", "main[data-report='checkout'] div:nth-child(" + index + ") > button#" + LONG_TOKEN)
                    .attribute("request.url", LONG_URL)
                    .attribute("correlation.uuid", UUID.nameUUIDFromBytes(("fixture-" + index).getBytes()).toString())
                    .build());
        }
        if (failing) {
            IllegalStateException exception = new IllegalStateException(
                    "Końcowy błąd <aplikacji> & komunikat z polskimi znakami: żółć; " + LONG_TOKEN);
            session.addEvent(TraceEvent.builder(TraceEventType.ASSERTION_FAILED, TraceStatus.FAILED,
                            "Końcowa asercja płatności")
                    .id("final-failure")
                    .timestamp(origin.plusSeconds(30))
                    .message(exception.getMessage())
                    .failure(TraceFailure.from(exception, true))
                    .attribute("expected", "Zapisano")
                    .attribute("actual", "Nie zapisano — " + LONG_TOKEN)
                    .build());
        }
        session.attachScreenshot("Zrzut czytelności", screenshot);
        session.attachArtifact(TraceArtifact.url("Długi URL", TraceArtifactType.CUSTOM_URL, LONG_URL));
        session.attachArtifact(TraceArtifact.networkLog("Brakujący log sieci", screenshot.resolveSibling("missing-network.json")));
        if (failing) {
            session.finishFailed(new AssertionError("Końcowa porażka sesji"));
        } else {
            session.finishPassed();
        }
        return session;
    }

    record FixturePaths(Path passLight,
                        Path passDark,
                        Path passAuto,
                        Path failLight,
                        Path failDark,
                        Path failAuto,
                        Path suiteLight,
                        Path suiteDark,
                        Path suiteAuto,
                        Path bundle) {
    }
}
