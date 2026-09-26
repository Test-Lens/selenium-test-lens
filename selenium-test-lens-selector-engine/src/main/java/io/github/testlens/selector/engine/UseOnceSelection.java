package io.github.testlens.selector.engine;

import java.util.Objects;

/** Ephemeral Lab/Audit choice; deliberately absent from persisted policy documents. */
public record UseOnceSelection(String decisionContextId, SelectorSubject subject, SelectorPolicy.Decision decision) {
    public UseOnceSelection { if(decisionContextId==null||decisionContextId.isBlank())throw new IllegalArgumentException("decisionContextId required"); Objects.requireNonNull(subject); Objects.requireNonNull(decision); }
}
