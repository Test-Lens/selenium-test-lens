package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Targeted compilation/execution result consumed by verifier and stabilizer roles. @since 0.5.0 */
public record TestExecutionResult(ContractHeader header,String scenarioId,Outcome compileOutcome,Outcome executionOutcome,
                                  String frameworkResult,long durationMillis,List<String>compileEvidenceRefs,
                                  List<String>traceEvidenceRefs,List<String>assertionEvidenceRefs,
                                  List<String>runtimeEventRefs,List<String>screenshotRefs,
                                  List<String>selectorDiagnosticRefs,String failureSummary){
    public TestExecutionResult(ContractHeader header,String scenarioId,Outcome compileOutcome,Outcome executionOutcome,
                               List<String>traceEvidenceRefs,List<String>screenshotRefs,
                               List<String>selectorDiagnosticRefs,String failureSummary){
        this(header,scenarioId,compileOutcome,executionOutcome,null,0,List.of(),traceEvidenceRefs,List.of(),
                List.of(),screenshotRefs,selectorDiagnosticRefs,failureSummary);
    }
    public TestExecutionResult{
        if(header==null||compileOutcome==null||executionOutcome==null)throw new IllegalArgumentException("header and outcomes are required");
        if(durationMillis<0)throw new IllegalArgumentException("durationMillis must not be negative");
        if(compileOutcome==Outcome.FAIL&&executionOutcome!=Outcome.NOT_RUN)throw new IllegalArgumentException("Execution must be NOT_RUN after compile failure");
        compileEvidenceRefs=canonical(compileEvidenceRefs);traceEvidenceRefs=canonical(traceEvidenceRefs);
        assertionEvidenceRefs=canonical(assertionEvidenceRefs);runtimeEventRefs=canonical(runtimeEventRefs);
        screenshotRefs=canonical(screenshotRefs);selectorDiagnosticRefs=canonical(selectorDiagnosticRefs);
    }
    public enum Outcome{PASS,FAIL,NOT_RUN,INCONCLUSIVE}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
