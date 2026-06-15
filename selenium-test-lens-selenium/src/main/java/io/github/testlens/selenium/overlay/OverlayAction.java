package io.github.testlens.selenium.overlay;

import org.openqa.selenium.By;

import java.util.Objects;

public final class OverlayAction {
    private final OverlayActionType type;
    private final By target;
    private final String reason;

    private OverlayAction(OverlayActionType type, By target, String reason) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.target = target;
        this.reason = reason;
    }

    public static OverlayAction click(By target) {
        return new OverlayAction(OverlayActionType.CLICK, requireTarget(target), null);
    }

    public static OverlayAction pressEscape() {
        return new OverlayAction(OverlayActionType.PRESS_ESCAPE, null, null);
    }

    public static OverlayAction waitUntilGone(By target) {
        return new OverlayAction(OverlayActionType.WAIT_UNTIL_GONE, requireTarget(target), null);
    }

    public static OverlayAction fail(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason must not be blank");
        }
        return new OverlayAction(OverlayActionType.FAIL, null, reason);
    }

    public OverlayActionType type() {
        return type;
    }

    public By target() {
        return target;
    }

    public String reason() {
        return reason;
    }

    public String describe() {
        return switch (type) {
            case CLICK -> "click(" + target + ")";
            case PRESS_ESCAPE -> "pressEscape";
            case WAIT_UNTIL_GONE -> "waitUntilGone(" + target + ")";
            case FAIL -> "fail";
        };
    }

    private static By requireTarget(By target) {
        return Objects.requireNonNull(target, "target must not be null");
    }
}
