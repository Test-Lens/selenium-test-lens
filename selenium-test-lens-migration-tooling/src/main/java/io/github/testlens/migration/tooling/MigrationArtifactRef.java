package io.github.testlens.migration.tooling;

import java.util.Objects;

/** Opaque bounded reference; path resolution stays in a trusted local registry. */
public record MigrationArtifactRef(Type type,int schemaVersion,String digest,String logicalRef,Completeness completeness){
    public MigrationArtifactRef{Objects.requireNonNull(type);if(schemaVersion<1)throw new IllegalArgumentException("schemaVersion");if(digest==null||!digest.matches("[a-z0-9-]+-v[0-9]+:sha256:[0-9a-f]{64}|sha256:[0-9a-f]{64}"))throw new IllegalArgumentException("digest");if(logicalRef==null||logicalRef.isBlank()||logicalRef.length()>256||logicalRef.indexOf('\0')>=0)throw new IllegalArgumentException("logicalRef");Objects.requireNonNull(completeness);}
    public enum Type{RUN_PLAN,BASELINE,COMPATIBILITY_MANIFEST,SELECTOR_AUDIT,STATIC_COMPATIBILITY,COMPATIBILITY_COMPARISON,PROPOSAL_SET,DECISIONS,OTHER}
    public enum Completeness{COMPLETE,PARTIAL,UNKNOWN}
}
