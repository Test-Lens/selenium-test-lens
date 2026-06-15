package io.github.testlens;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.testlens.core.OverlayLogger;
import io.github.testlens.core.logging.UiTestLensEventType;
import io.github.testlens.core.logging.UiTestLensLogEntry;
import io.github.testlens.core.logging.UiTestLensLogLevel;
import io.github.testlens.core.logging.UiTestLensStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

public class OverlayWait {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final JsOverlayDebug overlay;
    private final OverlayLogger logger;
    private final Clock clock;
    private static final DateTimeFormatter HUD_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z");

    public OverlayWait(WebDriver driver,
                       Duration timeout,
                       JsOverlayDebug overlay,
                       OverlayLogger logger) {
        this(driver, timeout, overlay, logger, Clock.systemDefaultZone());
    }

    public OverlayWait(WebDriver driver,
                       Duration timeout,
                       JsOverlayDebug overlay) {
        this(driver, timeout, overlay, OverlayLogger.noop(), Clock.systemDefaultZone());
    }

    OverlayWait(WebDriver driver,
                Duration timeout,
                JsOverlayDebug overlay,
                OverlayLogger logger,
                Clock clock) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
        this.overlay = overlay;
        this.logger = logger != null ? logger : OverlayLogger.noop();
        this.clock = clock != null ? clock : Clock.systemDefaultZone();
    }


    private WebElement waitInteractable(By by, long timeoutMs, String what) {
        long end = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < end) {
            try {
                WebElement el = driver.findElement(by);

                if (!el.isDisplayed() || !el.isEnabled()) {
                    sleepQuiet(120);
                    continue;
                }

                // real-world interactivity: pointer-events/visibility/opacity + elementFromPoint
                Boolean ok = (Boolean) ((JavascriptExecutor) driver).executeScript(
                        "var e=arguments[0];" +
                                "if(!e) return false;" +
                                "var cs=getComputedStyle(e);" +
                                "if(cs.pointerEvents==='none' || cs.visibility==='hidden' || cs.opacity==='0') return false;" +
                                "var r=e.getBoundingClientRect();" +
                                "if(r.width<2 || r.height<2) return false;" +
                                "var x=r.left + Math.min(r.width-1, Math.max(1, r.width/2));" +
                                "var y=r.top  + Math.min(r.height-1, Math.max(1, r.height/2));" +
                                "var top=document.elementFromPoint(x,y);" +
                                "return top===e || (top && e.contains(top));",
                        el
                );

                if (Boolean.TRUE.equals(ok)) {
                    return el; // zwracamy świeży element
                }

                sleepQuiet(120);
            } catch (NoSuchElementException e) {
                sleepQuiet(120);
            } catch (StaleElementReferenceException e) {
                // normalne w React – czekamy dalej i łapiemy świeży element w kolejnej iteracji
                sleepQuiet(120);
            }
        }

        throw new NoSuchElementException("Not interactable within timeout: " + what + " | locator=" + by);
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }


    public <T> T until(Function<? super WebDriver, T> condition, String description) {
        final String desc = (description == null || description.isBlank()) ? "waiting..." : description;
        final long startedMs = System.currentTimeMillis();

        safeLog(UiTestLensLogEntry.builder()
                .level(UiTestLensLogLevel.INFO)
                .eventType(UiTestLensEventType.WAIT)
                .status(UiTestLensStatus.STARTED)
                .message("WAIT STARTED | " + desc)
                .step("WAIT: " + desc)
                .action("wait")
                .metadata("description", desc)
                .build());

        safeOverlay(() -> {
            overlay.ensureWaitHudInjected();
            overlay.setStep("WAIT: " + desc);
            overlay.waitHudStart(desc);
        });

        Status status = Status.ERROR;
        try {
            T result = wait.until(condition);
            status = Status.DONE;
            return result;

        } catch (TimeoutException e) {
            status = Status.TIMEOUT;
            throw e;

        } finally {
            long elapsedMs = System.currentTimeMillis() - startedMs;

            // ===== TAGS / FORMAT =====
            String duration = formatDuration(elapsedMs); // mm:ss.SSS
            String tag = "[WAIT][" + status.prefix + "]"; // np. [WAIT][DONE]
            String msg = tag + " " + desc + " | duration=" + duration + " | elapsedMs=" + elapsedMs;

            // HUD step (stop indykatora + ewentualny step w HUD)
            Status finalStatus = status;
            safeOverlay(() -> overlay.waitHudStop(finalStatus.prefix + ": " + desc, elapsedMs));
            safeOverlay(() -> overlay.forceHideWaitHud());

            // HUD logs
            safeOverlay(() -> overlay.hudLog(
                    finalStatus.hudLevel,
                    msg,
                    timestamp()
            ));

            safeLog(finalStatus, desc, duration, elapsedMs);

        }

    }

    private String timestamp() {
        return HUD_TIMESTAMP_FORMATTER.format(ZonedDateTime.now(clock));
    }

    private static String formatDuration(long ms) {
        long total = Math.max(0, ms);
        long s = total / 1000;
        long mm = s / 60;
        long ss = s % 60;
        long mmm = total % 1000;
        return String.format("%02d:%02d.%03d", mm, ss, mmm);
    }


    private void safeOverlay(Runnable r) {
        if (overlay == null) return;
        try { r.run(); } catch (Exception ignored) {}
    }

    private void safeLog(Status status, String desc, String duration, long elapsedMs) {
        String msg = "WAIT " + status.prefix + " | " + desc + " | duration=" + duration + " | elapsedMs=" + elapsedMs;
        UiTestLensLogLevel level = switch (status) {
            case DONE -> UiTestLensLogLevel.INFO;
            case TIMEOUT -> UiTestLensLogLevel.WARN;
            case ERROR -> UiTestLensLogLevel.ERROR;
        };
        UiTestLensEventType eventType = status == Status.ERROR
                ? UiTestLensEventType.ERROR
                : UiTestLensEventType.WAIT;
        UiTestLensStatus lensStatus = status == Status.DONE
                ? UiTestLensStatus.PASSED
                : UiTestLensStatus.FAILED;

        safeLog(UiTestLensLogEntry.builder()
                .level(level)
                .eventType(eventType)
                .status(lensStatus)
                .message(msg)
                .step("WAIT: " + desc)
                .action("wait")
                .metadata("description", desc)
                .metadata("duration", duration)
                .metadata("elapsedMs", String.valueOf(elapsedMs))
                .metadata("status", status.prefix)
                .build());
    }

    private void safeLog(UiTestLensLogEntry entry) {
        try {
            logger.emit(entry);
        } catch (Exception ignored) {}
    }

    private enum Status {
        DONE("DONE", "success"),
        TIMEOUT("TIMEOUT", "warn"),
        ERROR("ERROR", "error");

        final String prefix;
        final String hudLevel;
        Status(String prefix, String hudLevel) {
            this.prefix = prefix;
            this.hudLevel = hudLevel;
        }
    }

    private static String fmtMs(long ms) {
        long total = Math.max(0, ms);
        long s = total / 1000;
        long mm = s / 60;
        long ss = s % 60;
        long mmm = total % 1000;
        return String.format("%02d:%02d.%03d", mm, ss, mmm);
    }

}
