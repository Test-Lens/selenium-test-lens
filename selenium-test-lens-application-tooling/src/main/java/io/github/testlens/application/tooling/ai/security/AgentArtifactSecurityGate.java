package io.github.testlens.application.tooling.ai.security;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.application.tooling.json.StrictJson;

import java.util.ArrayDeque;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

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

    /** Applies the full JSON/key policy while using generated-source rules only at explicit structural paths. */
    public Result validateStructuredAgentOutput(String text, RedactionPolicy policy,
                                                List<String> callerCanaries,
                                                Set<String> generatedSourcePaths) {
        return validateStructuredAgentOutput(text, policy, callerCanaries, generatedSourcePaths,
                (path, value) -> false);
    }

    /** Applies the full JSON/key policy while narrowly trusting validated values at explicit paths. */
    public Result validateStructuredAgentOutput(String text, RedactionPolicy policy,
                                                List<String> callerCanaries,
                                                Set<String> generatedSourcePaths,
                                                BiPredicate<String, String> trustedStructuredValue) {
        Objects.requireNonNull(policy, "policy");
        Objects.requireNonNull(trustedStructuredValue, "trustedStructuredValue");
        List<Finding> findings = new ArrayList<>();
        if (!policy.enabled()) {
            findings.add(new Finding("REDACTION_DISABLED", "Central redaction must be enabled"));
        }
        String value = text == null ? "" : text;
        if (policy.enabled()) {
            policy.inspectStructuredAgentOutput(value, generatedSourcePaths, trustedStructuredValue)
                    .forEach(finding -> findings.add(
                    new Finding(finding.rule(), "Sensitive content category=" + finding.category(),
                            finding.location())));
        }
        appendStructuredCanaryFindings(value, callerCanaries, findings);
        return result(findings);
    }

    /** Checks generated source without interpreting Java assignments as diagnostic key/value pairs. */
    public Result validateGeneratedSource(String source, RedactionPolicy policy, List<String> callerCanaries) {
        Objects.requireNonNull(policy, "policy");
        List<Finding> findings = new ArrayList<>();
        if (!policy.enabled()) {
            findings.add(new Finding("REDACTION_DISABLED", "Central redaction must be enabled"));
        } else {
            policy.inspectGeneratedSource(source).forEach(finding -> findings.add(new Finding(
                    finding.rule().name(), "Sensitive content category=" + finding.category(),
                    "$.payload.sourcePatch")));
        }
        appendCanaryFindings(source == null ? "" : source, callerCanaries, findings);
        return result(findings);
    }

    private static void appendCanaryFindings(String value, List<String> callerCanaries, List<Finding> findings) {
        for (String canary : callerCanaries == null ? List.<String>of() : callerCanaries) {
            if (canary != null && !canary.isEmpty() && value.contains(canary)) {
                findings.add(new Finding("CALLER_CANARY_PRESENT", "A caller-provided secret canary is present"));
            }
        }
    }

    private static void appendStructuredCanaryFindings(String value, List<String> callerCanaries,
                                                        List<Finding> findings) {
        List<String> canaries = (callerCanaries == null ? List.<String>of() : callerCanaries).stream()
                .filter(canary -> canary != null && !canary.isEmpty()).toList();
        if (canaries.isEmpty()) return;
        try {
            ArrayDeque<Object> pending = new ArrayDeque<>();
            pending.add(StrictJson.readObject(value.getBytes(StandardCharsets.UTF_8)));
            while (!pending.isEmpty()) {
                Object current = pending.removeFirst();
                if (current instanceof String string) {
                    if (canaries.stream().anyMatch(string::contains)) {
                        findings.add(new Finding("CALLER_CANARY_PRESENT",
                                "A caller-provided secret canary is present", "$.decodedString"));
                    }
                } else if (current instanceof java.util.Map<?, ?> map) {
                    for (var entry : map.entrySet()) {
                        String key = String.valueOf(entry.getKey());
                        if (canaries.stream().anyMatch(key::contains)) {
                            findings.add(new Finding("CALLER_CANARY_PRESENT",
                                    "A caller-provided secret canary is present", "$.decodedKey"));
                        }
                        if (entry.getValue() != null) pending.addLast(entry.getValue());
                    }
                } else if (current instanceof Iterable<?> iterable) {
                    for (Object item : iterable) if (item != null) pending.addLast(item);
                }
            }
        } catch (RuntimeException malformed) {
            appendCanaryFindings(value, canaries, findings);
        }
    }

    private static Result result(List<Finding> findings) {
        List<Finding> canonical = findings.stream()
                .distinct()
                .sorted(Comparator.comparing(Finding::code).thenComparing(Finding::location)
                        .thenComparing(Finding::detail))
                .toList();
        return new Result(canonical.isEmpty() ? Status.PASS : Status.BLOCKED, canonical);
    }

    public record Result(Status status, List<Finding> findings) {
        public Result {
            Objects.requireNonNull(status, "status");
            findings = List.copyOf(findings == null ? List.of() : findings);
        }
    }

    public record Finding(String code, String detail, String location) {
        public Finding(String code, String detail) {
            this(code, detail, "text");
        }

        public Finding {
            if (code == null || code.isBlank() || detail == null || detail.isBlank()
                    || location == null || location.isBlank()) {
                throw new IllegalArgumentException("code, detail, and location are required");
            }
            if (location.length() > 160) location = location.substring(0, 147) + ".[truncated]";
        }
    }

    public enum Status { PASS, BLOCKED }
}
