package io.github.testlens.selenium.steps;

import io.github.testlens.core.OverlayLogger;

import java.time.Instant;
import java.util.Objects;
import java.util.function.Consumer;

public final class UiStepScope {
    private final UiStepReporter reporter;
    private final Consumer<String> stepLabelSink;
    private final Consumer<String> hudLogSink;
    private final UiStepContext context = new UiStepContext();

    public UiStepScope(OverlayLogger logger, Consumer<String> stepLabelSink, Consumer<String> hudLogSink) {
        this.reporter = new UiStepReporter(logger);
        this.stepLabelSink = stepLabelSink;
        this.hudLogSink = hudLogSink;
    }

    public UiStepResult run(String name, UiStepOptions options, Runnable body) {
        UiStep step = new UiStep(name, body, options);
        UiStepOptions effectiveOptions = step.options();
        Instant started = Instant.now();
        if (effectiveOptions.logToHud() && stepLabelSink != null) {
            safeAccept(stepLabelSink, step.name());
        }
        reporter.started(step.name(), effectiveOptions);
        context.push(step.name());
        try {
            step.body().run();
            UiStepResult result = UiStepResult.passed(step.name(), started, Instant.now());
            reporter.finished(result, effectiveOptions);
            if (effectiveOptions.logToHud() && hudLogSink != null) {
                safeAccept(hudLogSink, "Step passed: " + step.name());
            }
            return result;
        } catch (RuntimeException | AssertionError e) {
            UiStepResult result = UiStepResult.failed(step.name(), started, Instant.now(), UiStepFailure.from(e, effectiveOptions));
            reporter.finished(result, effectiveOptions);
            if (effectiveOptions.logToHud() && hudLogSink != null) {
                safeAccept(hudLogSink, "Step failed: " + step.name());
            }
            if (effectiveOptions.failFast()) {
                throw new UiStepError(result);
            }
            return result;
        } finally {
            context.pop();
        }
    }

    public UiStepContext context() {
        return context;
    }

    private static void safeAccept(Consumer<String> consumer, String value) {
        Objects.requireNonNull(consumer, "consumer must not be null");
        try {
            consumer.accept(value);
        } catch (RuntimeException ignored) {
        }
    }
}

