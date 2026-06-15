package io.github.testlens.selenium.business;

import io.github.testlens.core.OverlayLogger;
import io.github.testlens.selenium.assertions.UiAssertionError;
import io.github.testlens.selenium.assertions.UiAssertionResult;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class BusinessAssertions {
    private final String subject;
    private final BusinessAssertionOptions options;
    private final BusinessAssertionReporter reporter;
    private final List<BusinessAssertion> checks = new ArrayList<>();
    private final List<BusinessAssertionResult> results = new ArrayList<>();
    private boolean verified;

    public BusinessAssertions(String subject, BusinessAssertionOptions options, OverlayLogger logger) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        this.subject = subject.trim();
        this.options = options != null ? options : BusinessAssertionOptions.defaults();
        this.reporter = new BusinessAssertionReporter(logger);
    }

    public BusinessAssertions check(String description, Runnable assertion) {
        ensureNotVerified();
        checks.add(BusinessAssertion.of(description, assertion));
        return this;
    }

    public BusinessAssertions check(String description, Supplier<UiAssertionResult> assertion) {
        ensureNotVerified();
        checks.add(BusinessAssertion.of(description, assertion));
        return this;
    }

    public BusinessAssertionResult verify() {
        verified = true;
        reporter.groupStarted(subject, checks.size());
        for (BusinessAssertion check : checks) {
            BusinessAssertionResult result = execute(check);
            results.add(result);
            reporter.checkFinished(result);
            if (result.status() == BusinessAssertionStatus.FAILED && (options.failFast() || !options.collectFailures())) {
                break;
            }
        }

        long failed = results.stream().filter(result -> result.status() == BusinessAssertionStatus.FAILED).count();
        reporter.groupFinished(subject, failed == 0, results.size(), (int) failed);
        if (failed > 0) {
            throw new BusinessAssertionError(subject, results, options);
        }
        return BusinessAssertionResult.passed(subject, subject, totalElapsed());
    }

    public String subject() {
        return subject;
    }

    public List<BusinessAssertionResult> results() {
        return Collections.unmodifiableList(results);
    }

    private BusinessAssertionResult execute(BusinessAssertion check) {
        Objects.requireNonNull(check, "check must not be null");
        reporter.checkStarted(subject, check.description());
        Instant started = Instant.now();
        try {
            UiAssertionResult assertionResult = check.run();
            Duration elapsed = Duration.between(started, Instant.now());
            if (assertionResult != null && !assertionResult.isPassed()) {
                BusinessAssertionFailure failure = BusinessAssertionFailure.fromAssertion(subject, check.description(),
                        assertionResult, null, elapsed);
                return BusinessAssertionResult.failed(subject, check.description(), failure, elapsed);
            }
            return BusinessAssertionResult.passed(subject, check.description(), elapsed);
        } catch (UiAssertionError e) {
            Duration elapsed = Duration.between(started, Instant.now());
            BusinessAssertionFailure failure = BusinessAssertionFailure.fromAssertion(subject, check.description(),
                    e.result(), e, elapsed);
            return BusinessAssertionResult.failed(subject, check.description(), failure, elapsed);
        } catch (RuntimeException | AssertionError e) {
            Duration elapsed = Duration.between(started, Instant.now());
            BusinessAssertionFailure failure = BusinessAssertionFailure.unexpected(subject, check.description(), e,
                    elapsed, options.messagePreviewLimit());
            return BusinessAssertionResult.failed(subject, check.description(), failure, elapsed);
        }
    }

    private Duration totalElapsed() {
        return results.stream()
                .map(BusinessAssertionResult::elapsed)
                .reduce(Duration.ZERO, Duration::plus);
    }

    private void ensureNotVerified() {
        if (verified) {
            throw new IllegalStateException("Business assertions have already been verified.");
        }
    }
}
