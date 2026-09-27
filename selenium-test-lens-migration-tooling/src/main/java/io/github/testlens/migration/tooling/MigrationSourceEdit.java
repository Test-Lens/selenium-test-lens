package io.github.testlens.migration.tooling;

import java.util.Objects;

/** Exact UTF-16 source edit. It is applied only to an in-memory preview in S10B2. */
public record MigrationSourceEdit(String logicalPath, int startUtf16, int endUtf16Exclusive,
                                  String originalConstructDigest, String replacementText,
                                  String semanticBeforeRef, String semanticAfterRef) {
    public MigrationSourceEdit {
        Objects.requireNonNull(logicalPath); Objects.requireNonNull(originalConstructDigest);
        Objects.requireNonNull(replacementText); Objects.requireNonNull(semanticBeforeRef); Objects.requireNonNull(semanticAfterRef);
        if(startUtf16<0||endUtf16Exclusive<startUtf16||replacementText.length()>8*1024*1024)throw new IllegalArgumentException("edit");
    }
}
