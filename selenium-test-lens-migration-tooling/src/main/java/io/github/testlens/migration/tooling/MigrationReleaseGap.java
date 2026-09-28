package io.github.testlens.migration.tooling;

import java.util.Objects;

/** Structured, non-executable release-hardening gap handed from S11 to S12. */
public record MigrationReleaseGap(String gapCode,Stage stage,Status status,String reason,Criticality criticality,
        String owner,String evidenceRef){
    public MigrationReleaseGap{gapCode=safe(gapCode,128);Objects.requireNonNull(stage);Objects.requireNonNull(status);reason=safe(reason,1024);Objects.requireNonNull(criticality);owner=safe(owner,64);if(evidenceRef!=null)MigrationApplyPlan.require(evidenceRef,evidenceRef.startsWith("sha256:")?"sha256":evidenceRef.substring(0,evidenceRef.indexOf(":sha256:")));}
    public enum Stage{S11,S12_RELEASE,S12_SCALE,S12_BROWSER,S12_WINDOWS,S12_DOCS,S12_MAINTENANCE}
    public enum Status{NOT_RUN,SKIPPED,DEFERRED,KNOWN_LIMITATION}
    public enum Criticality{RELEASE_BLOCKER,HARDENING_REQUIRED,DOCUMENTED_LIMITATION,OPTIONAL_ENVIRONMENT_VALIDATION}
    private static String safe(String v,int n){if(v==null||v.isBlank()||v.length()>n||v.indexOf('\0')>=0)throw new IllegalArgumentException("bounded gap value");return v;}
}
