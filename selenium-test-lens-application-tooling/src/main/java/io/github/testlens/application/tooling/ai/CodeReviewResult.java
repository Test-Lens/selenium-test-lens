package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;

/** Structured test-code review result. @since 0.5.0 */
public record CodeReviewResult(ContractHeader header,List<Finding>findings){public CodeReviewResult{if(header==null)throw new IllegalArgumentException("header is required");findings=(findings==null?List.<Finding>of():findings).stream().sorted(Comparator.comparing(Finding::severity).thenComparing(Finding::code).thenComparing(Finding::location)).toList();}
    public record Finding(Severity severity,String code,String location,String detail,String recommendation){}
    public enum Severity{ERROR,WARNING,INFO}
}
