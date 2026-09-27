package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Validated neutral evidence projection. Imported display text is never patch material. */
public record MigrationEvidence(MigrationEvidenceRef ref, Kind kind, List<Fact> facts, List<String> limitations) {
    public static final int MAX_FACTS = 100_000, MAX_LIMITATIONS = 512;
    public MigrationEvidence {
        Objects.requireNonNull(ref); Objects.requireNonNull(kind);
        facts = List.copyOf(facts == null ? List.of() : facts);
        limitations = List.copyOf(limitations == null ? List.of() : limitations);
        if (facts.size() > MAX_FACTS || limitations.size() > MAX_LIMITATIONS) throw new IllegalArgumentException("evidence bound");
    }
    public enum Kind { COMPATIBILITY_COMPARISON, STATIC_COMPATIBILITY, SELECTOR_AUDIT }
    public record Fact(String code, String category, String state, String subjectRef, List<String> reasonCodes) {
        public Fact { Objects.requireNonNull(code); reasonCodes = List.copyOf(reasonCodes == null ? List.of() : reasonCodes); }
    }
}
