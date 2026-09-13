package io.github.testlens.allure;

import java.util.List;

/**
 * Immutable diagnostics from an Allure attachment attempt. Publication failures remain data and never replace the
 * runner's original test outcome.
 *
 * @param status aggregate outcome
 * @param attachedCount attachments successfully streamed to Allure
 * @param skippedCount selected artifacts that were unavailable or skipped
 * @param missingArtifacts fixed, non-sensitive names of unavailable artifacts
 * @param failures safe integration diagnostics without artifact contents
 * @since 0.3.0
 */
public record AllureAttachResult(AllureAttachStatus status, int attachedCount, int skippedCount,
                                 List<String> missingArtifacts, List<String> failures) {
    public AllureAttachResult {
        missingArtifacts = missingArtifacts == null ? List.of() : List.copyOf(missingArtifacts);
        failures = failures == null ? List.of() : List.copyOf(failures);
    }
}
