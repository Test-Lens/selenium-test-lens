package io.github.mmaciekk111.uitestlens.core;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensEventType;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogEntry;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensLogLevel;
import io.github.mmaciekk111.uitestlens.core.logging.UiTestLensStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Guards {

    private final WebDriver driver;
    private final OverlayLogger logger;

    private boolean enabled = boolProp("guard.enabled", true);
    private boolean failFast = boolProp("guard.failFast", true);

    private int bodySampleLimit = intProp("guard.bodySampleLimit", 1200);

    // “abstrakcyjne i zabawne” nie tu – tu są twarde tripwire’y na awarie
    private final List<String> needles = new ArrayList<>(List.of(
            "503 service unavailable",
            "502 bad gateway",
            "504 gateway time-out",
            "504 gateway timeout",
            "service unavailable",
            "bad gateway",
            "gateway timeout",
            "temporarily unavailable",
            "an error has occurred",
            "application error",
            "something went wrong",
            "chunkloaderror",
            "loading chunk",
            "failed to fetch"
    ));

    public Guards(WebDriver driver, OverlayLogger logger) {
        this.driver = driver;
        this.logger = logger != null ? logger : OverlayLogger.noop();
    }

    public Guards(WebDriver driver) {
        this(driver, OverlayLogger.noop());
    }

    public Guards setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public Guards setFailFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public Guards setBodySampleLimit(int bodySampleLimit) {
        this.bodySampleLimit = Math.max(200, bodySampleLimit);
        return this;
    }

    public Guards addNeedle(String needle) {
        if (needle != null && !needle.isBlank()) {
            needles.add(needle.toLowerCase(Locale.ROOT));
        }
        return this;
    }

    /**
     * Checkpoint: wykrywa typowe “padło 503 / crash SPA / bramka”.
     * Domyślnie: enabled + failFast (rzuca AssertionError).
     */
    public GuardResult checkpoint(String label) {
        if (!enabled) return GuardResult.ok(label);

        Snapshot s = snapshot();

        String hay = (s.title + "\n" + s.bodyText).toLowerCase(Locale.ROOT);

        String hit = null;
        for (String n : needles) {
            if (hay.contains(n)) {
                hit = n;
                break;
            }
        }

        boolean isLikelyErrorPage = hit != null;

        GuardResult r = new GuardResult(
                label,
                isLikelyErrorPage,
                hit,
                s.url,
                s.title,
                s.bodyText
        );

        if (r.isProblem) {
            String msg = r.formatForException();
            emitCheckpoint(r, UiTestLensLogLevel.ERROR, UiTestLensEventType.ERROR, UiTestLensStatus.FAILED, msg);

            if (failFast) throw new AssertionError(msg);
        } else {
            emitCheckpoint(r, UiTestLensLogLevel.DEBUG, UiTestLensEventType.GENERAL, UiTestLensStatus.PASSED, "GUARD OK");
        }

        return r;
    }

    /** Wersja stricte assertująca (czytelność w scenariuszach). */
    public void assertOk(String label) {
        checkpoint(label); // checkpoint już rzuca AssertionError, jeśli failFast=true
    }

    // =========================
    // Internal
    // =========================

    private Snapshot snapshot() {
        String url = safe(() -> driver.getCurrentUrl());
        String title = safe(() -> driver.getTitle());

        String body = "";
        try {
            if (driver instanceof JavascriptExecutor js) {
                Object v = js.executeScript(
                        "try {" +
                                "  var t = document && document.body ? (document.body.innerText || document.body.textContent || '') : '';" +
                                "  t = String(t || '');" +
                                "  return t.length > arguments[0] ? t.substring(0, arguments[0]) : t;" +
                                "} catch(e) { return ''; }",
                        bodySampleLimit
                );
                body = v == null ? "" : String.valueOf(v);
            }
        } catch (Exception ignored) {}

        body = normalizeWhitespace(body);

        return new Snapshot(url, title, body);
    }

    private static String normalizeWhitespace(String s) {
        if (s == null) return "";
        // nie rób z tego “prettifiera” – tylko redukcja chaosu
        return s.replace('\u00A0', ' ')
                .replaceAll("[\\t\\r\\f]+", " ")
                .replaceAll(" +", " ")
                .trim();
    }

    private static <T> T safe(SupplierEx<T> s) {
        try { return s.get(); } catch (Exception e) { return null; }
    }

    private static boolean boolProp(String key, boolean def) {
        String v = System.getProperty(key);
        if (v == null) return def;
        return "1".equals(v) || "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v) || "on".equalsIgnoreCase(v);
    }

    private static int intProp(String key, int def) {
        try {
            String v = System.getProperty(key);
            if (v == null || v.isBlank()) return def;
            return Integer.parseInt(v.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private void emitCheckpoint(GuardResult result,
                                UiTestLensLogLevel level,
                                UiTestLensEventType eventType,
                                UiTestLensStatus status,
                                String message) {
        try {
            logger.emit(UiTestLensLogEntry.builder()
                    .level(level)
                    .eventType(eventType)
                    .status(status)
                    .message(message)
                    .action("guard.checkpoint")
                    .metadata("label", valueOrEmpty(result.label))
                    .metadata("hit", valueOrEmpty(result.hit))
                    .metadata("url", valueOrEmpty(result.url))
                    .metadata("title", valueOrEmpty(result.title))
                    .build());
        } catch (Exception ignored) {}
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @FunctionalInterface
    private interface SupplierEx<T> { T get() throws Exception; }

    private record Snapshot(String url, String title, String bodyText) {}

    public static final class GuardResult {
        public final String label;
        public final boolean isProblem;
        public final String hit;
        public final String url;
        public final String title;
        public final String bodySample;

        private GuardResult(String label,
                            boolean isProblem,
                            String hit,
                            String url,
                            String title,
                            String bodySample) {
            this.label = label;
            this.isProblem = isProblem;
            this.hit = hit;
            this.url = url;
            this.title = title;
            this.bodySample = bodySample;
        }

        public static GuardResult ok(String label) {
            return new GuardResult(label, false, null, null, null, null);
        }

        public String formatForException() {
            StringBuilder sb = new StringBuilder();
            sb.append("GUARD TRIPPED");
            if (label != null && !label.isBlank()) sb.append(" @ ").append(label);
            if (hit != null) sb.append(" | hit='").append(hit).append("'");
            sb.append("\nURL: ").append(url == null ? "-" : url);
            sb.append("\nTITLE: ").append(title == null ? "-" : title);
            sb.append("\nBODY(sample): ").append(bodySample == null ? "-" : bodySample);
            return sb.toString();
        }
    }
}

