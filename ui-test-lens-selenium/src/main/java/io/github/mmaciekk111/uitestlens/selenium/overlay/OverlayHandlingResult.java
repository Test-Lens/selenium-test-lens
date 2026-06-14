package io.github.mmaciekk111.uitestlens.selenium.overlay;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class OverlayHandlingResult {
    private final String handlerName;
    private final OverlayHandlingStatus status;
    private final List<String> attemptedActions;
    private final String message;
    private final Throwable exception;
    private final Duration elapsed;

    private OverlayHandlingResult(String handlerName,
                                  OverlayHandlingStatus status,
                                  List<String> attemptedActions,
                                  String message,
                                  Throwable exception,
                                  Duration elapsed) {
        this.handlerName = handlerName != null ? handlerName : "";
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.attemptedActions = List.copyOf(attemptedActions != null ? attemptedActions : List.of());
        this.message = message != null ? message : "";
        this.exception = exception;
        this.elapsed = elapsed != null ? elapsed : Duration.ZERO;
    }

    public static OverlayHandlingResult notDetected(String handlerName, Duration elapsed) {
        return new OverlayHandlingResult(handlerName, OverlayHandlingStatus.NOT_DETECTED, List.of(),
                "Overlay not detected", null, elapsed);
    }

    public static OverlayHandlingResult handled(String handlerName, List<String> attemptedActions, Duration elapsed) {
        return new OverlayHandlingResult(handlerName, OverlayHandlingStatus.HANDLED, attemptedActions,
                "Overlay handled", null, elapsed);
    }

    public static OverlayHandlingResult stillVisible(String handlerName, List<String> attemptedActions, Duration elapsed) {
        return new OverlayHandlingResult(handlerName, OverlayHandlingStatus.STILL_VISIBLE, attemptedActions,
                "Overlay is still visible", null, elapsed);
    }

    public static OverlayHandlingResult failed(String handlerName,
                                               List<String> attemptedActions,
                                               String message,
                                               Throwable exception,
                                               Duration elapsed) {
        return new OverlayHandlingResult(handlerName, OverlayHandlingStatus.FAILED, attemptedActions,
                message, exception, elapsed);
    }

    public static OverlayHandlingResult skipped(String handlerName, String message, Duration elapsed) {
        return new OverlayHandlingResult(handlerName, OverlayHandlingStatus.SKIPPED, List.of(),
                message, null, elapsed);
    }

    public String handlerName() {
        return handlerName;
    }

    public OverlayHandlingStatus status() {
        return status;
    }

    public List<String> attemptedActions() {
        return attemptedActions;
    }

    public String message() {
        return message;
    }

    public Throwable exception() {
        return exception;
    }

    public Duration elapsed() {
        return elapsed;
    }

    public boolean detected() {
        return status != OverlayHandlingStatus.NOT_DETECTED && status != OverlayHandlingStatus.SKIPPED;
    }
}
