package io.github.testlens.core.logging;

import java.lang.StackWalker.StackFrame;
import java.util.List;
import java.util.Optional;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Internal selector for the first meaningful consumer frame from the current Java stack. */
final class SourceLocationCapture {
    static final String CAPTURE_MARKER = "testlens.internal.captureSourceLocation";
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private static final List<String> INTERNAL_PREFIXES = List.of(
            "io.github.testlens.actions.", "io.github.testlens.api.", "io.github.testlens.core.",
            "io.github.testlens.hud.", "io.github.testlens.selenium.", "io.github.testlens.utils.",
            "io.github.testlens.react.", "io.github.testlens.junit5.", "io.github.testlens.testng.",
            "io.github.testlens.allure.", "org.openqa.selenium.", "org.junit.", "org.junit.jupiter.",
            "org.testng.", "java.", "javax.", "jdk.", "sun.", "com.sun.",
            "org.codehaus.groovy.runtime.", "net.bytebuddy.", "org.mockito.", "kotlin.");
    private static final Set<String> INTERNAL_ROOT_CLASSES = Set.of(
            "io.github.testlens.TestLens", "io.github.testlens.JsOverlayDebug",
            "io.github.testlens.OverlayConfig", "io.github.testlens.TestLensOptions");
    private static final java.util.concurrent.atomic.AtomicInteger CAPTURE_INVOCATIONS =
            new java.util.concurrent.atomic.AtomicInteger();

    private SourceLocationCapture() {}

    static Optional<SourceLocation> capture() {
        CAPTURE_INVOCATIONS.incrementAndGet();
        return WALKER.walk(frames -> select(frames.toList()));
    }

    static int captureInvocations() { return CAPTURE_INVOCATIONS.get(); }

    static void resetCaptureInvocations() { CAPTURE_INVOCATIONS.set(0); }

    static UiTestLensLogEntry enrich(UiTestLensLogEntry entry) {
        if (entry == null || !"true".equals(entry.metadata().get(CAPTURE_MARKER))) return entry;
        Map<String, String> metadata = new LinkedHashMap<>(entry.metadata());
        metadata.remove(CAPTURE_MARKER);
        UiTestLensLogEntry.Builder builder = entry.toBuilder().metadata(metadata);
        capture().ifPresent(builder::sourceLocation);
        return builder.build();
    }

    static Optional<SourceLocation> select(Iterable<? extends StackFrame> frames) {
        if (frames == null) return Optional.empty();
        for (StackFrame frame : frames) {
            if (isConsumerFrame(frame)) {
                return Optional.of(new SourceLocation(frame.getClassName(), frame.getMethodName(),
                        frame.getFileName(), frame.getLineNumber()));
            }
        }
        return Optional.empty();
    }

    static boolean isConsumerFrame(StackFrame frame) {
        if (frame == null || frame.getFileName() == null || frame.getLineNumber() <= 0) return false;
        String className = frame.getClassName();
        if (className == null || className.isBlank()) return false;
        boolean internalRoot = INTERNAL_ROOT_CLASSES.stream()
                .anyMatch(root -> className.equals(root) || className.startsWith(root + "$"));
        return !internalRoot && INTERNAL_PREFIXES.stream().noneMatch(className::startsWith)
                && !className.contains("$$") && !className.startsWith("com.sun.proxy.$Proxy");
    }
}
