package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Common versioned evidence envelope for provider-neutral agent artifacts. @since 0.5.0 */
public record ContractHeader(int schemaVersion, Status status, List<String> evidence,
                             List<String> limitations, Confidence confidence) {
    public static final int SCHEMA_VERSION=1;
    public ContractHeader{if(schemaVersion!=SCHEMA_VERSION)throw new IllegalArgumentException("Unsupported schemaVersion");if(status==null||confidence==null)throw new IllegalArgumentException("status and confidence are required");evidence=canonical(evidence);limitations=canonical(limitations);}
    private static List<String>canonical(List<String>values){return(values==null?List.<String>of():values).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    public enum Status{READY,PARTIAL,BLOCKED,COMPLETED,FAILED,UNKNOWN}
    public enum Confidence{OBSERVED,LIVE_VALIDATED,USER_DECLARED,INFERRED,AI_PROPOSED,UNKNOWN}
}
