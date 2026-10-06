package io.github.testlens.application.tooling.ai.workflow;

import java.util.List;

/** Reviewable proposal; it never mutates Page Object sources. @since 0.5.0 */
public record PageObjectExtensionProposal(PageObjectCapabilityMissing missing, String extensionClass,
                                          String proposedMethod, List<String> evidence) {
    public PageObjectExtensionProposal {
        if (missing == null || blank(extensionClass) || blank(proposedMethod)) throw new IllegalArgumentException("proposal fields are required");
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
