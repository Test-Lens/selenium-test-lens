package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;

/** Structured test-code review result. @since 0.5.0 */
public record CodeReviewResult(ContractHeader header,Verdict verdict,List<String>reviewedArtifactRefs,List<Finding>findings){
    public CodeReviewResult(ContractHeader header,List<Finding>findings){this(header,infer(findings),List.of(),findings);}
    public CodeReviewResult{if(header==null||verdict==null)throw new IllegalArgumentException("header and verdict are required");reviewedArtifactRefs=(reviewedArtifactRefs==null?List.<String>of():reviewedArtifactRefs).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();findings=(findings==null?List.<Finding>of():findings).stream().sorted(Comparator.comparing(Finding::severity).thenComparing(Finding::code).thenComparing(Finding::location)).toList();}
    private static Verdict infer(List<Finding> findings){return findings!=null&&findings.stream().anyMatch(value->value.severity()==Severity.ERROR)?Verdict.REQUEST_CHANGES:Verdict.APPROVE;}
    public record Finding(Severity severity,String code,String location,String detail,String recommendation){}
    public enum Severity{ERROR,WARNING,INFO}
    public enum Verdict{APPROVE,REQUEST_CHANGES,BLOCK}
}
