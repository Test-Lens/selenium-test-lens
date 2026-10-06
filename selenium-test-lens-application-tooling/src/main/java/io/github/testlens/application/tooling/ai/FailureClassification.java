package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Evidence-backed failure classification; UNKNOWN is preferable to an unsupported selector diagnosis. @since 0.5.0 */
public record FailureClassification(ContractHeader header,Category category,String rootCause,List<String>affectedIds,List<String>evidenceRefs){public FailureClassification{if(header==null||category==null)throw new IllegalArgumentException("header and category are required");affectedIds=canonical(affectedIds);evidenceRefs=canonical(evidenceRefs);}
    public enum Category{PRODUCT_DEFECT,TEST_LOGIC_DEFECT,SELECTOR_INSTABILITY,TIMING_SYNCHRONIZATION,ASSERTION_EXPECTATION,TEST_DATA,ENVIRONMENT,AUTHENTICATION,PAGE_MODEL_DRIFT,UNKNOWN}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
