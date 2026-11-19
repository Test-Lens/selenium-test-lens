package io.github.testlens.studio;

/** A valid Implementer domain result that cannot proceed to compilation or execution. */
public final class PageObjectCapabilityException extends IllegalStateException {
    public enum RefusalCategory {
        PAGE_OBJECT_METHOD_MISSING, PAGE_OBJECT_CONSTRUCTION_MISSING, BROWSER_SESSION_BINDING_MISSING,
        TEST_SETUP_MISSING, TEST_DATA_MISSING, ASSERTION_INCOMPATIBLE, CONTEXT_INCOMPLETE,
        CAPABILITY_PRESENT_BUT_REJECTED, UNKNOWN
    }
    private final String reasonCode;
    private final String location;
    private final String nextAction;
    private final RefusalCategory refusalCategory;
    private final String verifiedPageId;
    private final boolean sourceFingerprintMatched;
    private final java.util.Map<String,String> diagnosticMetadata;

    PageObjectCapabilityException(String reasonCode, String location) {
        this(reasonCode, location, RefusalCategory.UNKNOWN, null, false, java.util.Map.of());
    }

    PageObjectCapabilityException(String reasonCode, String location, RefusalCategory refusalCategory,
                                  String verifiedPageId, boolean sourceFingerprintMatched) {
        this(reasonCode, location, refusalCategory, verifiedPageId, sourceFingerprintMatched, java.util.Map.of());
    }

    PageObjectCapabilityException(String reasonCode, String location, RefusalCategory refusalCategory,
                                  String verifiedPageId, boolean sourceFingerprintMatched,
                                  java.util.Map<String,String> diagnosticMetadata) {
        super(safeMessage(reasonCode));
        this.reasonCode = reasonCode;
        this.location = location;
        this.nextAction = "REVISE_REQUIREMENT_OR_PAGE_OBJECT";
        this.refusalCategory = java.util.Objects.requireNonNull(refusalCategory, "refusalCategory");
        this.verifiedPageId = verifiedPageId;
        this.sourceFingerprintMatched = sourceFingerprintMatched;
        this.diagnosticMetadata = java.util.Map.copyOf(diagnosticMetadata == null
                ? java.util.Map.of() : diagnosticMetadata);
    }

    private static String safeMessage(String reasonCode) {
        String safe = reasonCode != null && reasonCode.matches("[A-Z][A-Z0-9_]{0,63}")
                ? reasonCode : "UNSPECIFIED_FAILURE";
        return "The Implementer requires a Page Object capability before test generation can continue [" + safe + ']';
    }

    static PageObjectCapabilityException sourceVerification(
            io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator.SourceVerification verification) {
        boolean fingerprintMatched = "MATCHED".equals(verification.safeMetadata().get("fingerprintStatus"));
        return new PageObjectCapabilityException(verification.reason(), verification.location(),
                RefusalCategory.UNKNOWN, null, fingerprintMatched, verification.safeMetadata());
    }

    static PageObjectCapabilityException staleManifest(String location) {
        return new PageObjectCapabilityException(
                io.github.testlens.application.tooling.ai.workflow.runner.PageObjectApiReferenceValidator.MANIFEST_STALE,
                location, RefusalCategory.UNKNOWN, null, false, java.util.Map.of(
                "validationStage", "SOURCE_API_VERIFICATION", "ruleId", "MANIFEST_FINGERPRINT",
                "problemCategory", "MANIFEST", "astNodeKind", "MANIFEST",
                "receiverCategory", "NOT_APPLICABLE", "manifestMatch", "STALE",
                "fingerprintStatus", "MISMATCH", "signatureStatus", "NOT_EVALUATED"));
    }

    static PageObjectCapabilityException manifestPreflight(
            io.github.testlens.application.tooling.ai.PageObjectManifestReadiness.Assessment assessment) {
        java.util.Map<String,String> metadata=new java.util.LinkedHashMap<>(assessment.safeMetadata());
        metadata.put("validationStage","MANIFEST_PREFLIGHT");
        metadata.put("ruleId","MANIFEST_READINESS");
        metadata.put("problemCategory","MANIFEST");
        metadata.put("astNodeKind","MANIFEST");
        metadata.put("receiverCategory","NOT_APPLICABLE");
        metadata.put("manifestMatch",assessment.ready()?"MATCHED":assessment.status()
                ==io.github.testlens.application.tooling.ai.PageObjectManifestReadiness.Status.MANIFEST_STALE?"STALE":"INCOMPLETE");
        metadata.put("fingerprintStatus",assessment.status()
                ==io.github.testlens.application.tooling.ai.PageObjectManifestReadiness.Status.MANIFEST_STALE
                ||assessment.status()==io.github.testlens.application.tooling.ai.PageObjectManifestReadiness.Status.FINGERPRINT_MISMATCH
                ?"MISMATCH":"MATCHED");
        metadata.put("signatureStatus","NOT_EVALUATED");
        return new PageObjectCapabilityException(assessment.reasonCode(),
                "$.context.pageObjectCapabilities",RefusalCategory.CONTEXT_INCOMPLETE,null,
                "MATCHED".equals(metadata.get("fingerprintStatus")),java.util.Map.copyOf(metadata));
    }

    public String reasonCode() { return reasonCode; }
    public String location() { return location; }
    public String nextAction() { return nextAction; }
    public RefusalCategory refusalCategory() { return refusalCategory; }
    public String verifiedPageId() { return verifiedPageId; }
    public boolean sourceFingerprintMatched() { return sourceFingerprintMatched; }
    public java.util.Map<String,String> diagnosticMetadata() { return diagnosticMetadata; }
}
