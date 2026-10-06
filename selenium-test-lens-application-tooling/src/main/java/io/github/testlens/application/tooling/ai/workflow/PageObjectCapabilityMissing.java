package io.github.testlens.application.tooling.ai.workflow;

/** Evidence that implementation cannot proceed through the Page Object API. @since 0.5.0 */
public record PageObjectCapabilityMissing(String pageId, String capability, String reason) {
    public PageObjectCapabilityMissing {
        if (blank(pageId) || blank(capability) || blank(reason)) throw new IllegalArgumentException("all fields are required");
    }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
}
