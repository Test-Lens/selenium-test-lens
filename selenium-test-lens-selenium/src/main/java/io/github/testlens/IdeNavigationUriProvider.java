package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

/** Creates escaped browser protocol targets without invoking a shell or IDE process. */
final class IdeNavigationUriProvider {
    private static final java.util.concurrent.atomic.AtomicInteger TARGET_INVOCATIONS =
            new java.util.concurrent.atomic.AtomicInteger();
    private IdeNavigationUriProvider() {}

    static Optional<String> target(SourceNavigationOptions options, Path file, int line, Integer column) {
        TARGET_INVOCATIONS.incrementAndGet();
        if (options == null || file == null) return Optional.empty();
        Path absolute = file.toAbsolutePath().normalize();
        int safeLine = Math.max(1, line);
        int safeColumn = column == null ? 1 : Math.max(1, column);
        if (options.ide() == SourceIde.INTELLIJ) {
            return Optional.of("idea://open?file=" + encode(absolute.toString()) + "&line=" + safeLine);
        }
        if (options.ide() == SourceIde.VSCODE) {
            String rawPath = absolute.toUri().getRawPath();
            return Optional.of("vscode://file" + rawPath + ":" + safeLine + ":" + safeColumn);
        }
        String template = options.customUriTemplate();
        if (template == null || template.isBlank()) return Optional.empty();
        return Optional.of(template.replace("{file}", encode(absolute.toString()))
                .replace("{line}", String.valueOf(safeLine)).replace("{column}", String.valueOf(safeColumn)));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static void resetMetrics() { TARGET_INVOCATIONS.set(0); }
    static int targetInvocationCount() { return TARGET_INVOCATIONS.get(); }
}
