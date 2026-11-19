package io.github.testlens.application.tooling.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** One deterministic readiness contract shared by Studio preflight and source-derived API verification. */
public final class PageObjectManifestReadiness {
    public enum Status { READY, SOURCE_INDEX_INCOMPLETE, REQUIRED_CLASS_MISSING, REQUIRED_API_UNRESOLVED,
        CONSTRUCTION_UNRESOLVED, CORRELATION_INSUFFICIENT, MANIFEST_TRUNCATED,
        FINGERPRINT_MISMATCH, MANIFEST_STALE }

    public static final Set<String> REJECTION_REASON_CODES = java.util.Arrays.stream(Status.values())
            .filter(status -> status != Status.READY)
            .map(PageObjectManifestReadiness::reasonCode)
            .collect(Collectors.toUnmodifiableSet());

    public record Assessment(Status status, AgentContextPack.ManifestProvenance provenance) {
        public boolean ready() { return status == Status.READY; }
        public String reasonCode() { return PageObjectManifestReadiness.reasonCode(status); }
        public Map<String, String> safeMetadata() {
            AgentContextPack.ManifestProvenance value = provenance == null
                    ? AgentContextPack.ManifestProvenance.empty() : provenance;
            Map<String, String> result = new LinkedHashMap<>();
            result.put("manifestOriginStage", value.originStage().name());
            result.put("manifestReason", status.name());
            result.put("sourceIndexCompleteness", value.sourceIndexCompleteness());
            result.put("selectedPageObjectCount", Integer.toString(value.selectedPageObjectCount()));
            result.put("verifiedClassCount", Integer.toString(value.verifiedClassCount()));
            result.put("verifiedMethodCount", Integer.toString(value.verifiedMethodCount()));
            result.put("unresolvedClassCount", Integer.toString(value.unresolvedClassCount()));
            result.put("constructorStatus", value.constructionStatus().name());
            result.put("inheritedApiStatus", value.inheritedApiStatus().name());
            result.put("correlationStatus", value.correlationStatus().name());
            result.put("manifestTruncated", Boolean.toString(value.truncated()).toUpperCase(java.util.Locale.ROOT));
            result.put("manifestMaxClasses", Integer.toString(value.maxClasses()));
            result.put("manifestMaxMethods", Integer.toString(value.maxMethods()));
            result.put("manifestMaxCharacters", Integer.toString(value.maxSerializedCharacters()));
            boolean changed = value.sourcesChanged() || status == Status.MANIFEST_STALE
                    || status == Status.FINGERPRINT_MISMATCH;
            result.put("manifestSourcesChanged", Boolean.toString(changed).toUpperCase(java.util.Locale.ROOT));
            return Map.copyOf(result);
        }
    }

    private PageObjectManifestReadiness() { }

    public static String reasonCode(Status status) {
        return status == Status.READY ? "READY" : "PAGE_OBJECT_" + status.name();
    }

    public static Assessment assess(AgentContextPack.PageObjectCapabilityManifest manifest,
                                    String currentFingerprint) {
        return assess(manifest, currentFingerprint, Set.of());
    }

    public static Assessment assess(AgentContextPack.PageObjectCapabilityManifest manifest,
                                    String currentFingerprint, Set<String> requiredMethodIds) {
        if (manifest == null) return new Assessment(Status.REQUIRED_CLASS_MISSING,
                AgentContextPack.ManifestProvenance.empty());
        AgentContextPack.ManifestProvenance provenance = manifest.provenance();
        if (manifest.classes().isEmpty()) return new Assessment(Status.REQUIRED_CLASS_MISSING, provenance);
        if (manifest.sourceFingerprint() == null || manifest.sourceFingerprint().isBlank()
                || provenance.indexFingerprint() == null || provenance.indexFingerprint().isBlank()
                || !Objects.equals(manifest.sourceFingerprint(), provenance.indexFingerprint())) {
            return new Assessment(Status.FINGERPRINT_MISMATCH, provenance);
        }
        if (currentFingerprint == null || currentFingerprint.isBlank()
                || !Objects.equals(manifest.sourceFingerprint(), currentFingerprint)) {
            return new Assessment(Status.MANIFEST_STALE, provenance);
        }
        if (manifest.completeness() != AgentContextPack.ManifestCompleteness.COMPLETE) {
            Status reason = provenance.partialReasons().stream().map(PageObjectManifestReadiness::status)
                    .findFirst().orElse(Status.SOURCE_INDEX_INCOMPLETE);
            return new Assessment(reason, provenance);
        }
        Set<String> required = requiredMethodIds == null ? Set.of() : Set.copyOf(requiredMethodIds);
        if (!required.isEmpty()) {
            Set<String> available = manifest.classes().stream().flatMap(value -> value.methods().stream())
                    .map(AgentContextPack.MethodCapability::methodId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!available.containsAll(required)) return new Assessment(Status.REQUIRED_API_UNRESOLVED, provenance);
        }
        return new Assessment(Status.READY, provenance);
    }

    private static Status status(AgentContextPack.ManifestPartialReason reason) {
        return Status.valueOf(reason.name());
    }
}
