package io.github.testlens.migration.tooling;

import java.util.Objects;

/** Content-addressed reference to validated migration evidence. */
public record MigrationEvidenceRef(Type type, int schemaVersion, int algorithmVersion, String canonicalDigest,
                                   String logicalArtifactRef, TrustClass trustClass,
                                   Completeness completeness, String provenance) {
    public MigrationEvidenceRef {
        Objects.requireNonNull(type); Objects.requireNonNull(trustClass); Objects.requireNonNull(completeness);
        if (schemaVersion != 1 || algorithmVersion != 1) throw new IllegalArgumentException("unsupported evidence version");
        if (canonicalDigest == null || !canonicalDigest.matches("sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("digest");
        if (logicalArtifactRef == null || !logicalArtifactRef.matches("[A-Za-z0-9][A-Za-z0-9._/-]{0,1023}")
                || logicalArtifactRef.contains("..") || logicalArtifactRef.startsWith("/") || logicalArtifactRef.contains(":"))
            throw new IllegalArgumentException("unsafe logical artifact ref");
        if (provenance == null || provenance.length() > 1024 || provenance.indexOf('\0') >= 0) throw new IllegalArgumentException("provenance");
    }
    public enum Type { COMPATIBILITY_COMPARISON, STATIC_COMPATIBILITY, SELECTOR_AUDIT, TRUSTED_CANDIDATE }
    public enum TrustClass { SANITIZED_EVIDENCE, TRUSTED_LOCAL_PATCH_MATERIAL, UNTRUSTED_IMPORTED_DATA }
    public enum Completeness { COMPLETE, PARTIAL, UNKNOWN }
}
