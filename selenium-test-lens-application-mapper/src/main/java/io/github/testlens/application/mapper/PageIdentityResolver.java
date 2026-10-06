package io.github.testlens.application.mapper;

import io.github.testlens.application.model.ApplicationIds;
import io.github.testlens.application.model.ApplicationModel;
import io.github.testlens.core.redaction.RedactionPolicy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Session-local deterministic page identity registry. It consumes only redacted, page-independent evidence. */
final class PageIdentityResolver {
    private static final double STATE_OVERLAP = 0.50d;
    private final String applicationId;
    private final ApplicationOverrides overrides;
    private final List<KnownIdentity> known = new ArrayList<>();

    PageIdentityResolver(String applicationId, ApplicationOverrides overrides) {
        this.applicationId = applicationId;
        this.overrides = overrides;
    }

    Resolution resolve(String normalizedUrl, String title, List<PageScanner.DiscoveredElement> elements,
                       List<String> preferredAttributes, RedactionPolicy redaction) {
        Set<String> signals = new HashSet<>();
        boolean testAttributeLandmark = false;
        for (PageScanner.DiscoveredElement element : elements) {
            TestLandmark configuredLandmark = preferredLandmark(element.testAttributes(), preferredAttributes, redaction);
            if (configuredLandmark != null) {
                signals.add(ApplicationIds.id("page-test-landmark-v1", SemanticNaming.type(element).name(),
                        configuredLandmark.attribute(), configuredLandmark.value()));
                testAttributeLandmark = true;
                continue;
            }
            String accessible=redaction.redact(element.accessibleName()),label=redaction.redact(element.label());
            String semantic = first(accessible, label,
                    redaction.redact(element.role()), SemanticNaming.type(element).name());
            String weakStructural=(accessible==null||accessible.isBlank())&&(label==null||label.isBlank())?redaction.redact(element.structuralHint()):"";
            signals.add(ApplicationIds.id("page-landmark-v1", SemanticNaming.type(element).name(), semantic,
                    redaction.redact(element.regionKey()), weakStructural));
        }
        List<String> ordered = signals.stream().sorted().toList();
        String observationFingerprint = ApplicationIds.id("page-observation-v1", ordered.toArray(String[]::new));
        String configuredGroup = overrides.pageIdentityGroupsByObservationFingerprint().get(observationFingerprint);
        if (configuredGroup != null && !configuredGroup.isBlank()) {
            String pageId = ApplicationIds.id("page-v2", applicationId, "configured", configuredGroup.trim());
            remember(pageId, normalizedUrl, title, signals, configuredGroup.trim());
            return new Resolution(pageId, observationFingerprint, configuredGroup.trim(), ApplicationModel.EvidenceSource.USER_DECLARED,
                    evidence("USER_IDENTITY_GROUP", testAttributeLandmark));
        }
        KnownIdentity match = known.stream()
                .filter(value -> value.group == null && value.normalizedUrl.equals(normalizedUrl))
                .filter(value -> significantOverlap(value.signals, signals)
                        || (value.signals.isEmpty() && signals.isEmpty() && equal(value.title, title)))
                .max(Comparator.comparingDouble(value -> overlap(value.signals, signals)))
                .orElse(null);
        if (match != null) {
            match.signals.addAll(signals);
            return new Resolution(match.pageId, observationFingerprint, null, ApplicationModel.EvidenceSource.OBSERVED,
                    evidence("SEMANTIC_LANDMARK_OVERLAP", testAttributeLandmark));
        }
        String pageId = ApplicationIds.id("page-v2", applicationId, normalizedUrl,
                title == null ? "" : title, observationFingerprint);
        remember(pageId, normalizedUrl, title, signals, null);
        return new Resolution(pageId, observationFingerprint, null, ApplicationModel.EvidenceSource.OBSERVED,
                evidence("TITLE", testAttributeLandmark));
    }

    Resolution resolve(String normalizedUrl, String title, List<PageScanner.DiscoveredElement> elements,
                       RedactionPolicy redaction) {
        return resolve(normalizedUrl, title, elements, List.of(), redaction);
    }

    private static List<String> evidence(String distinguishingEvidence, boolean testAttributeLandmark) {
        List<String> evidence = new ArrayList<>(List.of("URL_PATTERN", distinguishingEvidence, "SEMANTIC_LANDMARKS"));
        if (testAttributeLandmark) evidence.add("CONFIGURED_TEST_ATTRIBUTE_LANDMARKS");
        return List.copyOf(evidence);
    }

    private static String testAttribute(Map<String, String> attributes, String configuredAttribute) {
        if (attributes == null || configuredAttribute == null || configuredAttribute.isBlank()) return null;
        String exact = attributes.get(configuredAttribute);
        if (exact != null) return exact;
        for (Map.Entry<String, String> entry : attributes.entrySet())
            if (configuredAttribute.equalsIgnoreCase(entry.getKey())) return entry.getValue();
        return null;
    }

    private static TestLandmark preferredLandmark(Map<String, String> attributes, List<String> preferredAttributes,
                                                  RedactionPolicy redaction) {
        for (String configuredAttribute : preferredAttributes) {
            String value = testAttribute(attributes, configuredAttribute);
            if (value == null) continue;
            String redactedValue = redactAttributeValue(redaction, configuredAttribute, value);
            if (redactedValue != null && !redactedValue.isBlank())
                return new TestLandmark(configuredAttribute.toLowerCase(Locale.ROOT), redactedValue);
        }
        return null;
    }

    private static String redactAttributeValue(RedactionPolicy redaction, String attribute, String value) {
        String redacted = redaction.redact(attribute, value);
        if (!value.equals(redacted)) return redacted;
        String normalized = attribute.toLowerCase(Locale.ROOT);
        return normalized.startsWith("data-") && normalized.length() > 5
                ? redaction.redact(normalized.substring(5), value)
                : redacted;
    }

    private void remember(String pageId, String url, String title, Set<String> signals, String group) {
        KnownIdentity existing = known.stream().filter(value -> value.pageId.equals(pageId)).findFirst().orElse(null);
        if (existing != null) existing.signals.addAll(signals);
        else known.add(new KnownIdentity(pageId, url, title, new HashSet<>(signals), group));
    }

    private static boolean significantOverlap(Set<String> left, Set<String> right) {
        return !left.isEmpty() && !right.isEmpty() && overlap(left, right) >= STATE_OVERLAP;
    }

    private static double overlap(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) return 0d;
        Set<String> intersection = new HashSet<>(left); intersection.retainAll(right);
        return (double) intersection.size() / Math.min(left.size(), right.size());
    }

    private static boolean equal(String left, String right) { return left == null ? right == null : left.equals(right); }
    private static String first(String... values) { for (String value : values) if (value != null && !value.isBlank()) return value; return "unknown"; }

    record Resolution(String pageId, String observationFingerprint, String identityGroup, ApplicationModel.EvidenceSource source,
                      List<String> evidence) { }
    private record TestLandmark(String attribute, String value) { }
    private record KnownIdentity(String pageId, String normalizedUrl, String title, Set<String> signals, String group) { }
}
