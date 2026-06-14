package io.github.mmaciekk111.uitestlens.selenium.actionability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ActionabilityReport {
    private final List<ActionabilityResult> results;
    private final ActionabilityStatus status;

    public ActionabilityReport(List<ActionabilityResult> results) {
        this.results = Collections.unmodifiableList(new ArrayList<>(results == null ? List.of() : results));
        this.status = resolveStatus(this.results);
    }

    public static ActionabilityReport of(List<ActionabilityResult> results) {
        return new ActionabilityReport(results);
    }

    public List<ActionabilityResult> results() {
        return results;
    }

    public ActionabilityStatus status() {
        return status;
    }

    public boolean isReady() {
        return status == ActionabilityStatus.READY;
    }

    public Optional<ActionabilityResult> firstFailure() {
        return results.stream()
                .filter(result -> result.status() == ActionabilityStatus.FAILED
                        || result.status() == ActionabilityStatus.NOT_READY)
                .findFirst();
    }

    public String summary() {
        if (results.isEmpty()) {
            return "Actionability skipped: no checks were executed";
        }
        String failures = results.stream()
                .filter(result -> result.status() == ActionabilityStatus.FAILED
                        || result.status() == ActionabilityStatus.NOT_READY)
                .map(result -> result.checkType() + "=" + result.status()
                        + (result.failureReason() == null ? "" : "/" + result.failureReason())
                        + (result.message().isBlank() ? "" : " (" + result.message() + ")"))
                .collect(Collectors.joining("; "));
        if (!failures.isBlank()) {
            return "Actionability " + status + ": " + failures;
        }
        return "Actionability " + status + ": " + results.size() + " checks";
    }

    private static ActionabilityStatus resolveStatus(List<ActionabilityResult> results) {
        if (results == null || results.isEmpty()) {
            return ActionabilityStatus.SKIPPED;
        }
        if (results.stream().anyMatch(result -> result.status() == ActionabilityStatus.FAILED)) {
            return ActionabilityStatus.FAILED;
        }
        if (results.stream().anyMatch(result -> result.status() == ActionabilityStatus.NOT_READY)) {
            return ActionabilityStatus.NOT_READY;
        }
        if (results.stream().allMatch(result -> result.status() == ActionabilityStatus.SKIPPED)) {
            return ActionabilityStatus.SKIPPED;
        }
        return ActionabilityStatus.READY;
    }
}
