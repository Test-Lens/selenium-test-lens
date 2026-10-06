package io.github.testlens.application.tooling.ai;

import java.util.List;

/** Provider-neutral agent task with explicit model scope. @since 0.5.0 */
public record AgentTask(ContractHeader header, TaskType type, String requirement,
                        List<String> requiredPageIds, List<String> requiredStateIds,
                        List<String> requiredElementIds, List<String> constraints) {
    public AgentTask{if(header==null||type==null||requirement==null||requirement.isBlank())throw new IllegalArgumentException("header, type and requirement are required");requiredPageIds=canonical(requiredPageIds);requiredStateIds=canonical(requiredStateIds);requiredElementIds=canonical(requiredElementIds);constraints=canonical(constraints);}
    private static List<String>canonical(List<String>v){return(v==null?List.<String>of():v).stream().filter(java.util.Objects::nonNull).distinct().sorted().toList();}
    public enum TaskType{CREATE_TEST,DESIGN_SCENARIO,VERIFY_TEST,STABILIZE_TEST,REVIEW_TEST}
}
