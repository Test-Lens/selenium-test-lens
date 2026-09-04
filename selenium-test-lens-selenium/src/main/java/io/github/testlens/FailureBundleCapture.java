package io.github.testlens;

import io.github.testlens.core.trace.TraceArtifact;
import io.github.testlens.core.trace.TraceEvent;
import io.github.testlens.core.trace.TraceEventType;
import io.github.testlens.core.trace.TraceStatus;
import io.github.testlens.core.trace.UiTestLensSession;
import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selenium.evidence.FailureBundleOptions;
import io.github.testlens.selenium.network.NetworkDiagnostics;
import io.github.testlens.selenium.network.NetworkEvent;
import io.github.testlens.selenium.network.NetworkEventType;
import io.github.testlens.selenium.network.NetworkSummary;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.UnsupportedCommandException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Internal, per-finalization failure evidence collector. */
final class FailureBundleCapture {
    private static final List<String> COMPONENT_ORDER = List.of(
            "diagnosticScreenshot", "cleanScreenshot", "failure", "context", "diagnostics",
            "runtime", "configuration", "networkSummary", "browserConsole", "pageSource",
            "trace", "htmlReport");
    private final WebDriver driver;
    private final JsOverlayDebug delegate;
    private final UiTestLensSession session;
    private final TestLensOptions lensOptions;
    private final FailureBundleOptions options;
    private final RedactionPolicy redactionPolicy;
    private final Path sessionDirectory;
    private final Path bundleDirectory;
    private final Path archive;
    private final Instant capturedAt = Instant.now();
    private final List<TraceEvent> traceSnapshot;
    private final Map<String, Component> components = new TreeMap<>();
    private final List<Throwable> failures = new ArrayList<>();

    FailureBundleCapture(WebDriver driver, JsOverlayDebug delegate, UiTestLensSession session,
                         TestLensOptions lensOptions, Path sessionDirectory) {
        this.driver = driver;
        this.delegate = delegate;
        this.session = session;
        this.lensOptions = lensOptions;
        this.options = lensOptions.failureBundleOptions();
        this.redactionPolicy = lensOptions.redactionPolicy();
        this.sessionDirectory = sessionDirectory.toAbsolutePath().normalize();
        this.bundleDirectory = this.sessionDirectory.resolve("failure-bundle");
        this.archive = this.sessionDirectory.resolve("failure-bundle.zip");
        this.traceSnapshot = session.events();
    }

    List<Throwable> failures() { return List.copyOf(failures); }
    Path directory() { return bundleDirectory; }
    Path archive() { return archive; }

    Path captureDiagnosticScreenshot(boolean screenshotOnFailure) {
        if (!screenshotOnFailure) {
            component("diagnosticScreenshot", "SKIPPED", null, null,
                    "Disabled by screenshotOnFailure=false");
            return null;
        }
        if (!options.diagnosticScreenshot()) {
            component("diagnosticScreenshot", "SKIPPED", null, null,
                    "Disabled by failure bundle configuration");
            return null;
        }
        return captureScreenshot("diagnosticScreenshot", sessionDirectory.resolve("failure-diagnostic.png"), true);
    }

    void captureCleanScreenshot(boolean screenshotOnFailure) {
        if (!screenshotOnFailure) {
            component("cleanScreenshot", "SKIPPED", null, null,
                    "Disabled by screenshotOnFailure=false");
            return;
        }
        if (!options.cleanScreenshot()) {
            component("cleanScreenshot", "SKIPPED", null, null,
                    "Disabled by failure bundle configuration");
            return;
        }
        Object token;
        try {
            token = delegate.hideDebugArtifactsTemporarily();
        } catch (RuntimeException failure) {
            failed("cleanScreenshot", "Could not hide Test Lens artifacts", failure);
            return;
        }
        try {
            captureScreenshot("cleanScreenshot", bundleDirectory.resolve("failure-clean.png"), true);
        } finally {
            try {
                delegate.restoreDebugArtifacts(token);
            } catch (RuntimeException restoreFailure) {
                failures.add(restoreFailure);
                Component current = components.get("cleanScreenshot");
                if (current != null) {
                    components.put("cleanScreenshot", current.withMessage(
                            append(current.message, "HUD restore failed: " + messageFor(restoreFailure))));
                }
            }
        }
    }

    void captureRemaining(Throwable failure, boolean policyFailure) {
        runCollector("failure", () -> captureFailure(failure, policyFailure));
        if (options.context()) runCollector("context", this::captureContext); else skipped("context");
        if (options.diagnostics()) runCollector("diagnostics", this::captureDiagnostics); else skipped("diagnostics");
        if (options.runtimeMetadata()) runCollector("runtime", this::captureRuntime); else skipped("runtime");
        if (options.configurationSnapshot()) runCollector("configuration", this::captureConfiguration); else skipped("configuration");
        if (options.networkSummary()) runCollector("networkSummary", this::captureNetwork); else skipped("networkSummary");
        if (options.browserConsole()) runCollector("browserConsole", this::captureConsole); else skippedSensitive("browserConsole");
        if (options.pageSource()) runCollector("pageSource", this::capturePageSource); else skippedSensitive("pageSource");
    }

    void complete(Path trace, Path html) {
        fileComponent("trace", trace, "application/json", "Final trace export unavailable");
        fileComponent("htmlReport", html, "text/html", "Final HTML export unavailable");
        for (String name : COMPONENT_ORDER) {
            if (!components.containsKey(name)) component(name, "SKIPPED", null, null, "Not collected");
        }
        writeManifest();
        if (options.zipArchive()) createArchive();
    }

    private Path captureScreenshot(String componentName, Path destination, boolean attach) {
        if (!(driver instanceof TakesScreenshot screenshotDriver)) {
            component(componentName, "UNSUPPORTED", null, null,
                    "WebDriver does not implement TakesScreenshot");
            return null;
        }
        try {
            Files.createDirectories(destination.getParent());
            Path source = screenshotDriver.getScreenshotAs(OutputType.FILE).toPath();
            atomicCopy(source, destination);
            if (attach) session.attachArtifact(TraceArtifact.screenshot(
                    componentName.equals("diagnosticScreenshot") ? "Failure diagnostic" : "Failure clean",
                    destination).withMetadata("capturedAt", Instant.now().toString()));
            component(componentName, "CAPTURED", relative(destination), "image/png", "");
            return destination;
        } catch (IOException | RuntimeException captureFailure) {
            failed(componentName, "Screenshot capture failed", captureFailure);
            return null;
        }
    }

    private void captureFailure(Throwable failure, boolean policyFailure) {
        Map<String, Object> data = orderedMap();
        data.put("present", failure != null);
        data.put("policyFailure", policyFailure);
        if (failure == null) {
            data.put("message", "No Throwable was supplied to finishFailed(null)");
        } else {
            data.put("type", failure.getClass().getName());
            data.put("message", safe(failure.getMessage()));
            data.put("stackTrace", stackTrace(failure));
            data.put("suppressed", throwableList(List.of(failure.getSuppressed()), 8));
            data.put("causeChain", causeChain(failure, 12));
        }
        writeJsonComponent("failure", "failure.json", data);
    }

    private void captureContext() {
        Map<String, Object> data = orderedMap();
        data.put("currentUrl", probe(driver::getCurrentUrl));
        data.put("title", probe(driver::getTitle));
        data.put("currentWindowHandle", probe(driver::getWindowHandle));
        data.put("windowHandles", probe(() -> {
            Set<String> handles = driver.getWindowHandles();
            Map<String, Object> result = orderedMap();
            result.put("count", handles.size());
            result.put("values", handles.stream().sorted().toList());
            return result;
        }));
        data.put("windowSize", probe(() -> dimension(driver.manage().window().getSize())));
        data.put("viewport", probe(this::viewport));
        data.put("frameContext", frameContext());
        writeJsonComponent("context", "context.json", data);
    }

    private void captureDiagnostics() {
        Map<String, Object> data = orderedMap();
        data.put("lastLocator", lastEvent(Set.of(TraceEventType.LOCATOR_RESOLVE, TraceEventType.LOCATOR_ACTION)));
        data.put("lastAction", lastEvent(Set.of(TraceEventType.ACTION_STARTED, TraceEventType.ACTION_PASSED,
                TraceEventType.ACTION_FAILED)));
        data.put("lastRetry", lastEvent(Set.of(TraceEventType.RETRY)));
        data.put("lastAssertion", lastEvent(Set.of(TraceEventType.ASSERTION_STARTED,
                TraceEventType.ASSERTION_PASSED, TraceEventType.ASSERTION_FAILED)));
        data.put("lastActionability", lastEvent(Set.of(TraceEventType.ACTIONABILITY_CHECK)));
        data.put("lastOverlay", lastEvent(Set.of(TraceEventType.OVERLAY_DETECTED, TraceEventType.OVERLAY_HANDLED)));
        data.put("retrySummary", retrySummary());
        writeJsonComponent("diagnostics", "diagnostics.json", data);
    }

    private void captureRuntime() {
        Map<String, Object> data = orderedMap();
        if (driver instanceof HasCapabilities hasCapabilities) {
            data.put("browserName", hasCapabilities.getCapabilities().getBrowserName());
            data.put("browserVersion", hasCapabilities.getCapabilities().getBrowserVersion());
            data.put("platformName", String.valueOf(hasCapabilities.getCapabilities().getPlatformName()));
        } else {
            data.put("capabilitiesStatus", "UNSUPPORTED");
        }
        Package seleniumPackage = WebDriver.class.getPackage();
        data.put("seleniumVersion", seleniumPackage == null ? "unknown" : safe(seleniumPackage.getImplementationVersion()));
        data.put("javaVersion", System.getProperty("java.version", "unknown"));
        data.put("osName", System.getProperty("os.name", "unknown"));
        data.put("osVersion", System.getProperty("os.version", "unknown"));
        data.put("osArchitecture", System.getProperty("os.arch", "unknown"));
        data.put("windowSize", probe(() -> dimension(driver.manage().window().getSize())));
        data.put("viewport", probe(this::viewport));
        writeJsonComponent("runtime", "runtime.json", data);
    }

    private void captureConfiguration() {
        Map<String, Object> data = orderedMap();
        data.put("timeoutMs", lensOptions.locatorOptions().timeout().toMillis());
        data.put("pollIntervalMs", lensOptions.locatorOptions().pollInterval().toMillis());
        data.put("maxRetries", lensOptions.locatorOptions().maxRetries());
        data.put("maxRetriesMeaning", "maximum attempts");
        data.put("retryOnStaleElement", lensOptions.locatorOptions().retryOnStaleElement());
        data.put("retryOnClickIntercepted", lensOptions.locatorOptions().retryOnClickIntercepted());
        data.put("retryOnNotInteractable", lensOptions.locatorOptions().retryOnNotInteractable());
        data.put("retryOutcomePolicy", lensOptions.retryOutcomePolicy().name());
        data.put("allowedRetries", lensOptions.allowedRetries());
        data.put("screenshotOnFailure", lensOptions.screenshotOnFailure());
        data.put("failureBundleEnabled", options.enabled());
        data.put("diagnosticScreenshot", options.diagnosticScreenshot());
        data.put("cleanScreenshot", options.cleanScreenshot());
        data.put("pageSource", options.pageSource());
        data.put("browserConsole", options.browserConsole());
        data.put("cleanupHudOnFinish", lensOptions.cleanupHudOnFinish());
        data.put("networkCaptureMode", delegate.networkDiagnosticsSnapshot()
                .map(NetworkDiagnostics::captureMode).map(Enum::name).orElse("NOT_INITIALIZED"));
        data.put("redactionEnabled", redactionPolicy.enabled());
        data.put("redactionReplacement", redactionPolicy.replacement());
        data.put("additionalSensitiveKeys", redactionPolicy.additionalSensitiveKeyCount());
        data.put("literalSecrets", redactionPolicy.literalSecretCount());
        writeJsonComponent("configuration", "configuration.json", data);
    }

    private void captureNetwork() {
        Optional<NetworkDiagnostics> current = delegate.networkDiagnosticsSnapshot();
        if (current.isEmpty()) {
            Map<String, Object> data = orderedMap();
            data.put("captureMode", "NOT_INITIALIZED");
            data.put("requestedCaptureMode", "NOT_INITIALIZED");
            data.put("activeCaptureMode", "");
            data.put("status", "STOPPED");
            data.put("totalRequests", 0);
            data.put("totalResponses", 0);
            data.put("failedResponses", 0);
            data.put("failedRequests", 0);
            data.put("pending", 0);
            data.put("ignoredEvents", 0);
            data.put("droppedEvents", 0);
            data.put("responseStatuses", Map.of());
            writeJsonComponent("networkSummary", "network-summary.json", data);
            return;
        }
        try {
            NetworkDiagnostics diagnostics = current.get();
            NetworkSummary summary = diagnostics.summary();
            Map<String, Long> statuses = new TreeMap<>();
            for (NetworkEvent event : diagnostics.events()) {
                if (event.type() == NetworkEventType.RESPONSE && event.response() != null) {
                    statuses.merge(String.valueOf(event.response().status()), 1L, Long::sum);
                }
            }
            Map<String, Object> data = orderedMap();
            data.put("captureMode", diagnostics.captureMode().name());
            data.put("requestedCaptureMode", diagnostics.captureMode().name());
            data.put("activeCaptureMode", diagnostics.activeCaptureMode().map(Enum::name).orElse(""));
            data.put("status", summary.status().name());
            data.put("totalRequests", summary.totalRequests());
            data.put("totalResponses", summary.totalResponses());
            data.put("failedResponses", summary.failedResponses());
            data.put("failedRequests", summary.failedRequests());
            data.put("pending", Math.max(0, summary.totalRequests() - summary.totalResponses() - summary.failedRequests()));
            data.put("ignoredEvents", summary.ignoredEvents());
            data.put("droppedEvents", summary.droppedEvents());
            data.put("responseStatuses", statuses);
            writeJsonComponent("networkSummary", "network-summary.json", data);
        } catch (RuntimeException failure) {
            failed("networkSummary", "Network summary capture failed", failure);
        }
    }

    private void captureConsole() {
        try {
            List<LogEntry> all = new ArrayList<>(driver.manage().logs().get(LogType.BROWSER).getAll());
            if (all.isEmpty()) {
                component("browserConsole", "EMPTY", null, "application/json", "Browser log is empty");
                return;
            }
            boolean truncated = all.size() > options.maxConsoleEntries();
            List<Map<String, Object>> entries = all.stream().limit(options.maxConsoleEntries()).map(entry -> {
                Map<String, Object> item = orderedMap();
                item.put("timestamp", entry.getTimestamp());
                item.put("level", entry.getLevel().getName());
                item.put("message", entry.getMessage());
                return item;
            }).toList();
            Map<String, Object> data = orderedMap();
            data.put("entries", entries);
            data.put("totalAvailable", all.size());
            data.put("captured", entries.size());
            byte[] json = jsonBytes(data);
            if (json.length > options.maxTextArtifactBytes()) {
                component("browserConsole", "SKIPPED_TOO_LARGE", null, "application/json",
                        "Serialized browser log exceeds maxTextArtifactBytes");
                return;
            }
            Path path = bundleDirectory.resolve("browser-console.json");
            atomicWrite(path, json);
            component("browserConsole", truncated ? "TRUNCATED" : "CAPTURED", relative(path),
                    "application/json", truncated ? "Limited by maxConsoleEntries" : "");
        } catch (UnsupportedCommandException unsupported) {
            component("browserConsole", "UNSUPPORTED", null, "application/json", messageFor(unsupported));
        } catch (RuntimeException | IOException failure) {
            failed("browserConsole", "Browser console capture failed", failure);
        }
    }

    private void capturePageSource() {
        try {
            String source = driver.getPageSource();
            String redacted = redactionPolicy.redact(safe(source));
            byte[] bytes = redacted.getBytes(StandardCharsets.UTF_8);
            if (bytes.length == 0) {
                component("pageSource", "EMPTY", null, "text/html", "Page source is empty");
                return;
            }
            boolean truncated = bytes.length > options.maxTextArtifactBytes();
            byte[] stored = truncated ? truncateUtf8(redacted, options.maxTextArtifactBytes()) : bytes;
            Path path = bundleDirectory.resolve("page-source.html");
            atomicWrite(path, stored);
            component("pageSource", truncated ? "TRUNCATED" : "CAPTURED",
                    relative(path), "text/html", truncated ? "Limited by maxTextArtifactBytes" : "");
        } catch (RuntimeException | IOException failure) {
            failed("pageSource", "Page source capture failed", failure);
        }
    }

    private void writeJsonComponent(String componentName, String fileName, Object value) {
        try {
            byte[] bytes = jsonBytes(value);
            if (bytes.length > options.maxTextArtifactBytes()) {
                component(componentName, "SKIPPED_TOO_LARGE", null, "application/json",
                        "Serialized component exceeds maxTextArtifactBytes");
                return;
            }
            Path path = bundleDirectory.resolve(fileName);
            atomicWrite(path, bytes);
            component(componentName, bytes.length == 0 ? "EMPTY" : "CAPTURED", relative(path),
                    "application/json", "");
        } catch (IOException | RuntimeException failure) {
            failed(componentName, "Component write failed", failure);
        }
    }

    private void writeManifest() {
        try {
            Map<String, Object> root = orderedMap();
            root.put("schemaVersion", 1);
            root.put("sessionId", session.id());
            root.put("sessionName", session.metadata().name());
            root.put("finalStatus", session.metadata().status().name());
            root.put("capturedAt", capturedAt.toString());
            Map<String, Object> serialized = orderedMap();
            for (String name : COMPONENT_ORDER) serialized.put(name, components.get(name).asMap());
            root.put("components", serialized);
            atomicWrite(bundleDirectory.resolve("manifest.json"), jsonBytes(root));
        } catch (IOException | RuntimeException failure) {
            failures.add(failure);
        }
    }

    private void createArchive() {
        Path temporary = archive.resolveSibling(archive.getFileName() + ".tmp");
        try {
            Files.createDirectories(archive.getParent());
            Map<String, Path> entries = new TreeMap<>();
            addZipEntry(entries, "manifest.json", bundleDirectory.resolve("manifest.json"));
            for (Component component : components.values()) {
                if (!"CAPTURED".equals(component.status) && !"TRUNCATED".equals(component.status)) continue;
                if (component.path == null) continue;
                Path file = sessionDirectory.resolve(component.path).normalize();
                String name = component.path.replace('\\', '/');
                if (name.startsWith("failure-bundle/")) name = name.substring("failure-bundle/".length());
                addZipEntry(entries, name, file);
            }
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(temporary,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                for (Map.Entry<String, Path> entry : entries.entrySet()) {
                    zip.putNextEntry(new ZipEntry(entry.getKey()));
                    Files.copy(entry.getValue(), zip);
                    zip.closeEntry();
                }
            }
            atomicMove(temporary, archive);
        } catch (IOException | RuntimeException failure) {
            failures.add(failure);
            try { Files.deleteIfExists(temporary); } catch (IOException cleanupFailure) { failure.addSuppressed(cleanupFailure); }
        }
    }

    private void addZipEntry(Map<String, Path> entries, String name, Path path) throws IOException {
        String normalizedName = name.replace('\\', '/');
        if (normalizedName.startsWith("/") || normalizedName.contains("../") || normalizedName.equals("..")) {
            throw new IOException("Unsafe ZIP entry: " + name);
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(sessionDirectory) || Files.isSymbolicLink(normalized)
                || !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS)) return;
        entries.put(normalizedName, normalized);
    }

    private void fileComponent(String name, Path path, String mediaType, String unavailableMessage) {
        if (path == null || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            components.put(name, new Component("FAILED", null, -1, mediaType, unavailableMessage));
        } else {
            String relative = relative(path);
            long size;
            try { size = Files.size(path); } catch (IOException ignored) { size = -1; }
            components.put(name, new Component("CAPTURED", relative, size, mediaType, ""));
        }
    }

    private void component(String name, String status, String path, String mediaType, String message) {
        long size = -1;
        if (path != null) {
            try { size = Files.size(sessionDirectory.resolve(path)); } catch (IOException ignored) { size = -1; }
        }
        Component component = new Component(status, path, size, mediaType, safe(message));
        components.put(name, component);
        session.addEvent(TraceEvent.builder(TraceEventType.FAILURE_BUNDLE,
                        "FAILED".equals(status) ? TraceStatus.WARNING : TraceStatus.INFO,
                        "Failure bundle: " + name)
                .message(component.message)
                .attribute("component", name)
                .attribute("componentStatus", status)
                .attribute("path", safe(path))
                .attribute("sizeBytes", size < 0 ? "" : String.valueOf(size))
                .attribute("mediaType", safe(mediaType))
                .attribute("archive", "failure-bundle.zip")
                .build());
    }

    private void failed(String name, String message, Throwable failure) {
        failures.add(failure);
        component(name, failure instanceof UnsupportedCommandException ? "UNSUPPORTED" : "FAILED",
                null, null, message + ": " + messageFor(failure));
    }

    private void runCollector(String name, Runnable collector) {
        try {
            collector.run();
        } catch (RuntimeException failure) {
            if (!components.containsKey(name)) failed(name, "Component capture failed", failure);
            else failures.add(failure);
        }
    }
    private void skipped(String name) { component(name, "SKIPPED", null, null, "Disabled by failure bundle configuration"); }
    private void skippedSensitive(String name) { component(name, "SKIPPED", null, null, "Disabled by default because the data may contain secrets"); }

    private Map<String, Object> lastEvent(Set<TraceEventType> types) {
        return traceSnapshot.stream().filter(event -> types.contains(event.type()))
                .max(Comparator.comparing(TraceEvent::timestamp)).map(this::eventMap)
                .orElseGet(() -> unavailable("No matching trace event"));
    }

    private Map<String, Object> eventMap(TraceEvent event) {
        Map<String, Object> data = orderedMap();
        data.put("availability", "AVAILABLE");
        data.put("type", event.type().name());
        data.put("status", event.status().name());
        data.put("name", event.name());
        data.put("message", event.message());
        data.put("timestamp", event.timestamp().toString());
        data.put("durationMs", event.duration().toMillis());
        data.put("attributes", new TreeMap<>(event.attributes()));
        if (event.failure() != null) {
            data.put("failureType", event.failure().exceptionType());
            data.put("failureMessage", event.failure().message());
        }
        return data;
    }

    private Map<String, Object> retrySummary() {
        var summary = session.retrySummary();
        Map<String, Object> data = orderedMap();
        data.put("totalRetries", summary.totalRetries());
        data.put("timeLostMs", summary.timeLost().toMillis());
        data.put("flakyCandidate", summary.flakyCandidate());
        data.put("policy", summary.policy().name());
        data.put("policyTriggered", summary.policyTriggered());
        data.put("byAction", summary.byAction());
        data.put("byLocator", summary.byLocator());
        data.put("byException", summary.byException());
        return data;
    }

    private Map<String, Object> frameContext() {
        for (int index = traceSnapshot.size() - 1; index >= 0; index--) {
            TraceEvent event = traceSnapshot.get(index);
            String action = event.attributes().get("action");
            if (event.status() != TraceStatus.PASSED || action == null) continue;
            if (action.equals("context.frame")) return available("FRAME");
            if (action.equals("context.defaultContent")) return available("DEFAULT_CONTENT");
            if (action.equals("context.parentFrame")) return unknownFrame("Parent frame depth is not portable");
        }
        return unknownFrame("Selenium exposes no portable active-frame probe");
    }

    private Object viewport() {
        if (!(driver instanceof JavascriptExecutor executor)) throw new UnsupportedOperationException("JavascriptExecutor unavailable");
        return executor.executeScript("return {width:window.innerWidth,height:window.innerHeight};");
    }

    private static Map<String, Object> probe(Supplier<?> supplier) {
        try {
            Object value = supplier.get();
            Map<String, Object> result = orderedMap();
            result.put("status", value == null ? "EMPTY" : "AVAILABLE");
            result.put("value", value);
            return result;
        } catch (RuntimeException failure) {
            Map<String, Object> result = orderedMap();
            result.put("status", failure instanceof UnsupportedCommandException ? "UNSUPPORTED" : "FAILED");
            result.put("message", messageFor(failure));
            return result;
        }
    }

    private static Map<String, Object> dimension(Dimension dimension) {
        Map<String, Object> value = orderedMap();
        value.put("width", dimension.getWidth());
        value.put("height", dimension.getHeight());
        return value;
    }

    private static List<Map<String, Object>> causeChain(Throwable throwable, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        Throwable current = throwable.getCause();
        for (int depth = 0; current != null && depth < limit; depth++, current = current.getCause()) {
            Map<String, Object> item = orderedMap();
            item.put("type", current.getClass().getName());
            item.put("message", safe(current.getMessage()));
            result.add(item);
        }
        return result;
    }

    private static List<Map<String, Object>> throwableList(List<Throwable> failures, int limit) {
        return failures.stream().limit(limit).map(failure -> {
            Map<String, Object> item = orderedMap();
            item.put("type", failure.getClass().getName());
            item.put("message", safe(failure.getMessage()));
            return item;
        }).toList();
    }

    private String relative(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(sessionDirectory)) throw new IllegalArgumentException("Path is outside session directory");
        return sessionDirectory.relativize(normalized).toString().replace('\\', '/');
    }

    private byte[] jsonBytes(Object value) {
        return new Json().toJson(redactJsonValue(null, value)).getBytes(StandardCharsets.UTF_8);
    }

    private Object redactJsonValue(String key, Object value) {
        if (value instanceof String text) return redactionPolicy.redact(key, text);
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> safe = orderedMap();
            map.forEach((entryKey, entryValue) -> {
                String name = String.valueOf(entryKey);
                safe.put(name, redactJsonValue(name, entryValue));
            });
            return safe;
        }
        if (value instanceof Iterable<?> values) {
            List<Object> safe = new ArrayList<>();
            values.forEach(item -> safe.add(redactJsonValue(key, item)));
            return List.copyOf(safe);
        }
        return value;
    }

    private static byte[] truncateUtf8(String value, long maximumBytes) {
        StringBuilder result = new StringBuilder();
        long used = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            int bytes = codePoint <= 0x7f ? 1 : codePoint <= 0x7ff ? 2 : codePoint <= 0xffff ? 3 : 4;
            if (used + bytes > maximumBytes) break;
            result.appendCodePoint(codePoint);
            used += bytes;
            offset += Character.charCount(codePoint);
        }
        return result.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void atomicWrite(Path destination, byte[] bytes) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, bytes, StandardOpenOption.TRUNCATE_EXISTING);
            atomicMove(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void atomicCopy(Path source, Path destination) throws IOException {
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), destination.getFileName().toString(), ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            atomicMove(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void atomicMove(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Map<String, Object> orderedMap() { return new LinkedHashMap<>(); }
    private static Map<String, Object> available(Object value) { Map<String, Object> m = orderedMap(); m.put("availability", "AVAILABLE"); m.put("value", value); return m; }
    private static Map<String, Object> unavailable(String message) { Map<String, Object> m = orderedMap(); m.put("availability", "UNAVAILABLE"); m.put("message", message); return m; }
    private static Map<String, Object> unknownFrame(String message) { Map<String, Object> m = unavailable(message); m.put("value", "UNKNOWN"); return m; }
    private static String safe(String value) { return value == null ? "" : value; }
    private static String append(String first, String second) { return first == null || first.isBlank() ? second : first + "; " + second; }
    private static String messageFor(Throwable failure) { return failure.getMessage() == null || failure.getMessage().isBlank() ? failure.getClass().getSimpleName() : failure.getMessage(); }
    private static String stackTrace(Throwable failure) { StringWriter out = new StringWriter(); failure.printStackTrace(new PrintWriter(out)); return out.toString(); }

    private record Component(String status, String path, long size, String mediaType, String message) {
        Component withMessage(String value) { return new Component(status, path, size, mediaType, value); }
        Map<String, Object> asMap() {
            Map<String, Object> data = orderedMap();
            data.put("status", status);
            if (path != null) data.put("path", path);
            if (size >= 0) data.put("sizeBytes", size);
            if (mediaType != null) data.put("mediaType", mediaType);
            if (message != null && !message.isBlank()) data.put("message", message);
            return data;
        }
    }
}
