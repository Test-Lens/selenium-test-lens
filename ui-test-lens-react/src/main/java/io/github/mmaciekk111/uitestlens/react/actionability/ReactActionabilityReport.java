package io.github.mmaciekk111.uitestlens.react.actionability;

import io.github.mmaciekk111.uitestlens.selenium.actionability.ActionabilityReport;
import io.github.mmaciekk111.uitestlens.selenium.actionability.ActionabilityStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ReactActionabilityReport {
    private final ActionabilityReport baseReport;
    private final List<ReactReadinessResult> reactResults;

    public ReactActionabilityReport(ActionabilityReport baseReport, List<ReactReadinessResult> reactResults) {
        this.baseReport = baseReport;
        this.reactResults = Collections.unmodifiableList(new ArrayList<>(reactResults == null ? List.of() : reactResults));
    }

    public static ReactActionabilityReport of(ActionabilityReport baseReport, List<ReactReadinessResult> reactResults) {
        return new ReactActionabilityReport(baseReport, reactResults);
    }

    public ActionabilityReport baseReport() {
        return baseReport;
    }

    public List<ReactReadinessResult> reactResults() {
        return reactResults;
    }

    public boolean isReady() {
        return (baseReport == null || baseReport.isReady())
                && reactResults.stream().allMatch(result ->
                result.status() == ActionabilityStatus.READY || result.status() == ActionabilityStatus.SKIPPED);
    }

    public Optional<ReactReadinessResult> firstReactFailure() {
        return reactResults.stream()
                .filter(result -> result.status() == ActionabilityStatus.FAILED
                        || result.status() == ActionabilityStatus.NOT_READY)
                .findFirst();
    }

    public String summary() {
        if (baseReport != null && !baseReport.isReady()) {
            return "React actionability NOT_READY: base actionability failed: " + baseReport.summary();
        }
        String failures = reactResults.stream()
                .filter(result -> result.status() == ActionabilityStatus.FAILED
                        || result.status() == ActionabilityStatus.NOT_READY)
                .map(result -> result.checkType() + "=" + result.status()
                        + (result.failureReason() == null ? "" : "/" + result.failureReason())
                        + (result.message().isBlank() ? "" : " (" + result.message() + ")"))
                .collect(Collectors.joining("; "));
        if (!failures.isBlank()) {
            return "React actionability NOT_READY: " + failures;
        }
        return "React actionability READY: " + reactResults.size() + " React readiness checks";
    }
}
