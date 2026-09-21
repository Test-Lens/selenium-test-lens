package io.github.testlens;

import io.github.testlens.hud.SourceIde;
import io.github.testlens.hud.SourceNavigationOptions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

/** Creates escaped browser protocol targets without invoking a shell or IDE process. */
final class IdeNavigationUriProvider {
    private static final Logger LOGGER = Logger.getLogger(IdeNavigationUriProvider.class.getName());
    private static final java.util.concurrent.atomic.AtomicInteger TARGET_INVOCATIONS =
            new java.util.concurrent.atomic.AtomicInteger();
    private IdeNavigationUriProvider() {}

    static Optional<String> target(SourceNavigationOptions options, Path file, int line, Integer column) {
        return target(options, file, line, column, Path.of(""));
    }

    static Optional<String> target(SourceNavigationOptions options, Path file, int line, Integer column,
                                   Path executionRoot) {
        TARGET_INVOCATIONS.incrementAndGet();
        if (options == null || file == null || line < 1 || (column != null && column < 1)) return Optional.empty();
        Path absolute = file.toAbsolutePath().normalize();
        if (!java.nio.file.Files.isRegularFile(absolute)) return Optional.empty();
        if (options.ide() == SourceIde.INTELLIJ) {
            Optional<IntellijProjectContext> context = IntellijProjectContext.resolve(options, executionRoot);
            if (context.isEmpty() || !absolute.startsWith(context.get().root())) {
                LOGGER.fine("IntelliJ source navigation unavailable: configure a valid project name/root mapping");
                return Optional.empty();
            }
            String relative = context.get().root().relativize(absolute).toString().replace('\\', '/');
            if (relative.isBlank()) return Optional.empty();
            String location = relative + ':' + line + (column == null ? "" : ":" + column);
            return Optional.of("jetbrains://idea/navigate/reference?project=" + encode(context.get().name())
                    + "&path=" + encode(location));
        }
        if (options.ide() == SourceIde.VSCODE) {
            String rawPath = absolute.toUri().getRawPath();
            return Optional.of("vscode://file" + rawPath + ":" + line + ":" + (column == null ? 1 : column));
        }
        String template = options.customUriTemplate();
        if (template == null || template.isBlank()) return Optional.empty();
        return Optional.of(template.replace("{file}", encode(absolute.toString()))
                .replace("{line}", String.valueOf(line)).replace("{column}", String.valueOf(column == null ? 1 : column)));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static void resetMetrics() { TARGET_INVOCATIONS.set(0); }
    static int targetInvocationCount() { return TARGET_INVOCATIONS.get(); }
}
