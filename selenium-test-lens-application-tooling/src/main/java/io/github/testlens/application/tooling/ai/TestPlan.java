package io.github.testlens.application.tooling.ai;

import java.util.Comparator;
import java.util.List;

/** Structured output contract for test architecture and scenario design. @since 0.5.0 */
public record TestPlan(ContractHeader header,List<TestScenario>scenarios){public TestPlan{if(header==null)throw new IllegalArgumentException("header is required");scenarios=(scenarios==null?List.<TestScenario>of():scenarios).stream().sorted(Comparator.comparing(TestScenario::scenarioId)).toList();}
    public record TestScenario(String scenarioId,String title,Priority priority,List<String>preconditions,List<TestStep>steps,List<ExpectedResult>expected,List<String>requiredPageIds,List<String>requiredStateIds,List<String>requiredElementIds,List<String>requiredData,List<String>riskAreas,CoverageDisposition existingCoverage,List<String>existingTestRefs,List<String>knownUnknowns){
        public TestScenario(String scenarioId,String title,Priority priority,List<String>preconditions,List<TestStep>steps,List<ExpectedResult>expected,List<String>requiredPageIds,List<String>requiredStateIds,List<String>requiredElementIds,List<String>requiredData,List<String>riskAreas){this(scenarioId,title,priority,preconditions,steps,expected,requiredPageIds,requiredStateIds,requiredElementIds,requiredData,riskAreas,CoverageDisposition.AMBIGUOUS,List.of(),List.of());}
        public TestScenario{preconditions=canonical(preconditions);steps=List.copyOf(steps==null?List.of():steps);expected=List.copyOf(expected==null?List.of():expected);requiredPageIds=canonical(requiredPageIds);requiredStateIds=canonical(requiredStateIds);requiredElementIds=canonical(requiredElementIds);requiredData=canonical(requiredData);riskAreas=canonical(riskAreas);existingCoverage=existingCoverage==null?CoverageDisposition.AMBIGUOUS:existingCoverage;existingTestRefs=canonical(existingTestRefs);knownUnknowns=canonical(knownUnknowns);}}
    public record TestStep(int order,String action,String pageId,String elementId,String valueRef){public TestStep{if(order<1)throw new IllegalArgumentException("step order must be positive");}}
    public record ExpectedResult(String pageId,String stateId,String assertion,String evidenceRef){}
    public enum Priority{CRITICAL,HIGH,MEDIUM,LOW}
    public enum CoverageDisposition{NEW_TEST_REQUIRED,EXTEND_EXISTING_TEST,ALREADY_COVERED,AMBIGUOUS}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
}
