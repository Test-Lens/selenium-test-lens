package io.github.testlens.migration.tooling;

import java.util.Objects;

/** Strict source precondition consumed later by S11; B2 never writes the source. */
public record MigrationSourcePrecondition(String logicalPath, String fileSha256, Encoding encoding, Newline newline,
        String expectedNodeKind, String declarationRef, String constructIdentityDigest, CharacterRange exactCharacterRange,
        String originalConstructDigest, String currentSemanticDigest, String proposedSemanticDigest,
        SymlinkState symlinkState, String repositoryBindingRef, String worktreeBindingRef,
        String checkpointRef, String alreadyAppliedSemanticState) {
    public MigrationSourcePrecondition {
        Objects.requireNonNull(logicalPath); Objects.requireNonNull(fileSha256); Objects.requireNonNull(encoding);
        Objects.requireNonNull(newline); Objects.requireNonNull(expectedNodeKind); Objects.requireNonNull(declarationRef);
        Objects.requireNonNull(constructIdentityDigest); Objects.requireNonNull(exactCharacterRange);
        Objects.requireNonNull(originalConstructDigest); Objects.requireNonNull(currentSemanticDigest);
        Objects.requireNonNull(proposedSemanticDigest); Objects.requireNonNull(symlinkState);
        if (!fileSha256.matches("sha256:[0-9a-f]{64}")) throw new IllegalArgumentException("fileSha256");
    }
    public enum Encoding { UTF8, UTF8_BOM }
    public enum Newline { LF, CRLF }
    public enum SymlinkState { REGULAR_FILE, SYMBOLIC_LINK }
    public record CharacterRange(int startUtf16, int endUtf16Exclusive) {
        public CharacterRange { if(startUtf16<0||endUtf16Exclusive<startUtf16)throw new IllegalArgumentException("range"); }
    }
}
