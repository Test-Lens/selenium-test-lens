package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Targeted compilation/execution result consumed by verifier and stabilizer roles. @since 0.5.0 */
public record TestExecutionResult(ContractHeader header,String scenarioId,Outcome compileOutcome,Outcome executionOutcome,
                                  List<String>traceEvidenceRefs,List<String>screenshotRefs,List<String>selectorDiagnosticRefs,
                                  String failureSummary){public TestExecutionResult{if(header==null||compileOutcome==null||executionOutcome==null)throw new IllegalArgumentException("header and outcomes are required");traceEvidenceRefs=canonical(traceEvidenceRefs);screenshotRefs=canonical(screenshotRefs);selectorDiagnosticRefs=canonical(selectorDiagnosticRefs);}
    public enum Outcome{PASS,FAIL,NOT_RUN,INCONCLUSIVE}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
