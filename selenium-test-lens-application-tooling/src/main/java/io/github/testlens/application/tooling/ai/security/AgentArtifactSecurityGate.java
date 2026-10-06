package io.github.testlens.application.tooling.ai.security;

import io.github.testlens.core.redaction.RedactionPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Fail-closed boundary for text and references that may leave the trusted Test Lens tooling process.
 * It does not inspect image pixels and therefore rejects image/video references by default.
 * @since 0.5.0
 */
public final class AgentArtifactSecurityGate {
    private static final List<String> FORBIDDEN_REFERENCE_MARKERS = List.of(
            "auth-state", "auth_state", "storage-state", "storage_state", "cookie",
            "page-source", "page_source", "console", "surefire", "stdout", "stderr",
            ".png", ".jpg", ".jpeg", ".webp", ".mp4", ".webm");

    public Result validate(String text, List<String> artifactReferences, RedactionPolicy policy,
                           List<String> callerCanaries) {
        Objects.requireNonNull(policy, "policy");
        List<Finding> findings = new ArrayList<>();
        if (!policy.enabled()) {
            findings.add(new Finding("REDACTION_DISABLED", "Central redaction must be enabled"));
        }
        String value = text == null ? "" : text;
        if (policy.enabled() && !value.equals(policy.redact(value))) {
            findings.add(new Finding("UNREDACTED_TEXT", "Text changes when the configured policy is applied"));
        }
        for (String canary : callerCanaries == null ? List.<String>of() : callerCanaries) {
            if (canary != null && !canary.isEmpty() && value.contains(canary)) {
                findings.add(new Finding("CALLER_CANARY_PRESENT", "A caller-provided secret canary is present"));
            }
        }
        for (String reference : artifactReferences == null ? List.<String>of() : artifactReferences) {
            if (reference == null) continue;
            String normalized = reference.toLowerCase(Locale.ROOT).replace('\\', '/');
            if (FORBIDDEN_REFERENCE_MARKERS.stream().anyMatch(normalized::contains)) {
                findings.add(new Finding("FORBIDDEN_ARTIFACT_REFERENCE",
                        "Agent context must not reference auth state, raw runner output, browser source, console, image, or video artifacts"));
            }
        }
        List<Finding> canonical = findings.stream()
                .distinct()
                .sorted(Comparator.comparing(Finding::code).thenComparing(Finding::detail))
                .toList();
        return new Result(canonical.isEmpty() ? Status.PASS : Status.BLOCKED, canonical);
    }

    public record Result(Status status, List<Finding> findings) {
        public Result {
            Objects.requireNonNull(status, "status");
            findings = List.copyOf(findings == null ? List.of() : findings);
        }
    }

    public record Finding(String code, String detail) {
        public Finding {
            if (code == null || code.isBlank() || detail == null || detail.isBlank()) {
                throw new IllegalArgumentException("code and detail are required");
            }
        }
    }

    public enum Status { PASS, BLOCKED }
}
