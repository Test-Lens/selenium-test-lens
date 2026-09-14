package io.github.testlens.allure;

import io.github.testlens.TestLensFinalizationResult;
import io.github.testlens.core.trace.TraceStatus;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Streams already-finalized Test Lens evidence into the current Allure test or step. This adapter never accesses
 * WebDriver, never captures a raw screenshot, and does not change the Allure test status. Call it after
 * {@code finishPassed/finishFailed/finishSkipped} and before the runner closes the current Allure executable.
 * Calls are at-most-once for the same result object and Allure context, including parallel test execution.
 *
 * <pre>{@code
 * TestLensFinalizationResult finalized = lens.finishFailed(failure);
 * AllureAttachResult publication = AllureTestLens.attach(finalized);
 * }</pre>
 *
 * @since 0.3.0
 */
public final class AllureTestLens {
    private static final Map<TestLensFinalizationResult, Set<String>> ATTACHED =
            Collections.synchronizedMap(new WeakHashMap<>());

    private AllureTestLens() { }

    /**
     * Attaches evidence using the failure-focused defaults.
     * @param result completed Test Lens finalization result
     * @return non-throwing publication diagnostics
     * @since 0.3.0
     */
    public static AllureAttachResult attach(TestLensFinalizationResult result) {
        return attach(result, AllureTestLensOptions.defaults());
    }

    /**
     * Attaches selected evidence to the current Allure executable.
     * @param result completed Test Lens finalization result
     * @param options artifact selection policy
     * @return non-throwing publication diagnostics
     * @since 0.3.0
     */
    public static AllureAttachResult attach(TestLensFinalizationResult result, AllureTestLensOptions options) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(options, "options");
        AllureLifecycle lifecycle = Allure.getLifecycle();
        String context = lifecycle.getCurrentTestCaseOrStep().orElse(null);
        if (context == null) {
            return result(AllureAttachStatus.SKIPPED_NO_ACTIVE_CONTEXT, 0, 0, List.of(), List.of());
        }
        boolean failed = result.session() != null && result.session().metadata().status() == TraceStatus.FAILED;
        if (!failed && !options.nonFailedSessions()) {
            return result(AllureAttachStatus.SKIPPED_BY_POLICY, 0, 0, List.of(), List.of());
        }
        synchronized (ATTACHED) {
            Set<String> contexts = ATTACHED.computeIfAbsent(result, ignored -> new java.util.HashSet<>());
            if (!contexts.add(context)) {
                return result(AllureAttachStatus.SKIPPED_ALREADY_ATTACHED, 0, 0, List.of(), List.of());
            }
        }

        List<Artifact> artifacts = new ArrayList<>();
        if (options.diagnosticScreenshot()) artifacts.add(new Artifact("Test Lens — Diagnostic screenshot", "image/png", ".png", () -> result.failureScreenshot()));
        if (options.cleanScreenshot()) artifacts.add(new Artifact("Test Lens — Clean screenshot", "image/png", ".png", () -> result.cleanFailureScreenshot().orElse(null)));
        if (options.htmlReport()) artifacts.add(new Artifact("Test Lens — Report", "text/html", ".html", result::htmlReport));
        if (options.trace()) artifacts.add(new Artifact("Test Lens — Trace", "application/json", ".json", result::jsonReport));
        if (options.failureBundle()) artifacts.add(new Artifact("Test Lens — Failure bundle", "application/zip", ".zip", () -> result.failureBundleArchive().orElse(null)));

        int attached = 0;
        int skipped = 0;
        List<String> missing = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        for (Artifact artifact : artifacts) {
            Path path = artifact.path().get();
            if (path == null || !Files.isRegularFile(path)) {
                skipped++;
                missing.add(artifact.name());
                continue;
            }
            try (InputStream input = Files.newInputStream(path)) {
                lifecycle.addAttachment(artifact.name(), artifact.type(), artifact.extension(), input);
                attached++;
            } catch (Exception failure) {
                skipped++;
                failures.add(artifact.name() + ": " + failure.getClass().getSimpleName());
            }
        }
        AllureAttachStatus status = failures.isEmpty()
                ? (attached > 0 ? (skipped == 0 ? AllureAttachStatus.ATTACHED : AllureAttachStatus.PARTIALLY_ATTACHED)
                : AllureAttachStatus.SKIPPED_NO_AVAILABLE_ARTIFACTS)
                : (attached > 0 ? AllureAttachStatus.PARTIALLY_ATTACHED : AllureAttachStatus.FAILED);
        return result(status, attached, skipped, missing, failures);
    }

    private static AllureAttachResult result(AllureAttachStatus status, int attached, int skipped,
                                             List<String> missing, List<String> failures) {
        return new AllureAttachResult(status, attached, skipped, missing, failures);
    }

    private record Artifact(String name, String type, String extension, Supplier<Path> path) { }
}
