package io.github.testlens.studio.workspace;

import io.github.testlens.application.tooling.ai.*;
import io.github.testlens.application.tooling.ai.workflow.AgentExecutor;
import io.github.testlens.application.tooling.ai.workflow.runner.AgentProtocolCodec;
import io.github.testlens.application.tooling.json.StrictJson;

import java.time.Instant;
import java.util.*;

/** Versioned, artifact-first state needed to continue a reviewed Studio workflow after host restart. */
public record WorkflowSessionSnapshot(int schemaVersion,String workflowId,String requirement,State state,
        Instant createdAt,Instant updatedAt,String projectFingerprint,String sourceFingerprint,Freshness freshness,
        TestPlan plan,TestImplementationProposal implementation,TestExecutionResult execution,
        FailureClassification diagnosis,RepairProposal repair,String repairDecision,String repairApplyStatus,
        boolean resumed,List<String> limitations,AgentFailureSnapshot agentFailure,CodeReviewResult review,
        List<ReviewCycleSnapshot> reviewHistory,RunDiagnosticSnapshot runDiagnostic){
    public static final int SCHEMA_VERSION=1;
    public enum State{CREATED,PLAN_GENERATING,PLAN_READY_FOR_REVIEW,IMPLEMENTATION_GENERATING,
        IMPLEMENTATION_READY_FOR_REVIEW,EXECUTION_RUNNING,VERIFICATION_RUNNING,EXECUTION_FAILED,
        REPAIR_READY_FOR_REVIEW,REPAIR_REJECTED,REPAIR_APPLIED_VERIFICATION_PENDING,SUCCESS,FAILED,
        NEEDS_HUMAN_REVIEW,AGENT_FAILED,AGENT_EXECUTION_INTERRUPTED,EXECUTION_INTERRUPTED,CORRUPTED}
    public enum Freshness{FRESH,STALE,MISSING,INVALID}
    public WorkflowSessionSnapshot{
        if(schemaVersion!=SCHEMA_VERSION)throw new IllegalArgumentException("Unsupported workflow snapshot schema");
        if(workflowId==null||!workflowId.matches("[A-Za-z0-9._-]{1,160}")||requirement==null||requirement.isBlank()||state==null||createdAt==null||updatedAt==null||freshness==null)throw new IllegalArgumentException("Invalid workflow snapshot");
        limitations=List.copyOf(limitations==null?List.of():limitations);
        reviewHistory=List.copyOf(reviewHistory==null?List.of():reviewHistory).stream().limit(2).toList();
    }
    public WorkflowSessionSnapshot(int schemaVersion,String workflowId,String requirement,State state,
            Instant createdAt,Instant updatedAt,String projectFingerprint,String sourceFingerprint,Freshness freshness,
            TestPlan plan,TestImplementationProposal implementation,TestExecutionResult execution,
            FailureClassification diagnosis,RepairProposal repair,String repairDecision,String repairApplyStatus,
            boolean resumed,List<String> limitations){
        this(schemaVersion,workflowId,requirement,state,createdAt,updatedAt,projectFingerprint,sourceFingerprint,
                freshness,plan,implementation,execution,diagnosis,repair,repairDecision,repairApplyStatus,resumed,
                limitations,null,null,List.of(),null);
    }
    public WorkflowSessionSnapshot(int schemaVersion,String workflowId,String requirement,State state,
            Instant createdAt,Instant updatedAt,String projectFingerprint,String sourceFingerprint,Freshness freshness,
            TestPlan plan,TestImplementationProposal implementation,TestExecutionResult execution,
            FailureClassification diagnosis,RepairProposal repair,String repairDecision,String repairApplyStatus,
            boolean resumed,List<String> limitations,AgentFailureSnapshot agentFailure){
        this(schemaVersion,workflowId,requirement,state,createdAt,updatedAt,projectFingerprint,sourceFingerprint,
                freshness,plan,implementation,execution,diagnosis,repair,repairDecision,repairApplyStatus,resumed,
                limitations,agentFailure,null,List.of(),null);
    }
    public WorkflowSessionSnapshot(int schemaVersion,String workflowId,String requirement,State state,
            Instant createdAt,Instant updatedAt,String projectFingerprint,String sourceFingerprint,Freshness freshness,
            TestPlan plan,TestImplementationProposal implementation,TestExecutionResult execution,
            FailureClassification diagnosis,RepairProposal repair,String repairDecision,String repairApplyStatus,
            boolean resumed,List<String> limitations,AgentFailureSnapshot agentFailure,CodeReviewResult review,
            List<ReviewCycleSnapshot> reviewHistory){
        this(schemaVersion,workflowId,requirement,state,createdAt,updatedAt,projectFingerprint,sourceFingerprint,
                freshness,plan,implementation,execution,diagnosis,repair,repairDecision,repairApplyStatus,resumed,
                limitations,agentFailure,review,reviewHistory,null);
    }
    public record RunDiagnosticSnapshot(RunReason reason,RunStatus status,String location,
                                        boolean recoveryPersisted){
        public RunDiagnosticSnapshot{
            Objects.requireNonNull(reason,"reason");Objects.requireNonNull(status,"status");
            if(location==null||!location.matches("[A-Z_]{1,64}"))throw new IllegalArgumentException("Invalid run diagnostic location");
        }
    }
    public enum RunReason{START_PERSISTENCE_FAILED,EXECUTION_RESULT_UNKNOWN,POST_EXECUTION_PROCESSING_FAILED,
        THREAD_INTERRUPTED,RECOVERY_PERSISTENCE_FAILED}
    public enum RunStatus{NOT_STARTED,INTERRUPTED,RECOVERY_NOT_PERSISTED}
    public record ReviewCycleSnapshot(int implementationVersion,String implementationArtifactRef,
            String executionArtifactRef,String reviewArtifactRef,String compileOutcome,String executionOutcome,
            long durationMillis,CodeReviewResult review){
        public ReviewCycleSnapshot{
            if(implementationVersion<1||!safe(implementationArtifactRef,128)||!safe(executionArtifactRef,128)
                    ||!safe(reviewArtifactRef,128)||!safe(compileOutcome,64)||!safe(executionOutcome,64)
                    ||durationMillis<0||review==null)throw new IllegalArgumentException("Invalid review cycle snapshot");
        }
        private static boolean safe(String value,int limit){return value!=null&&!value.isBlank()&&value.length()<=limit
                &&value.matches("[A-Za-z0-9_.:-]+");}
    }
    public record AgentFailureSnapshot(String code,String stage,String reasonCode,String location,String message,
                                       Map<String,String> metadata){
        public AgentFailureSnapshot{
            if(!safe(code,64)||!safe(stage,64)||!safe(reasonCode,96)||!safeLocation(location,256)
                    ||message==null||message.isBlank()||message.length()>512)throw new IllegalArgumentException("Invalid agent failure snapshot");
            metadata=Map.copyOf(metadata==null?Map.of():metadata);
            if(metadata.size()>16||metadata.entrySet().stream().anyMatch(entry->!safe(entry.getKey(),64)
                    ||!entry.getValue().matches("[A-Za-z0-9_.-]{1,160}")))throw new IllegalArgumentException("Invalid agent failure metadata");
        }
        private static boolean safe(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max
                &&value.matches("[A-Za-z0-9_.$:/ -]+");}
        private static boolean safeLocation(String value,int max){return value!=null&&!value.isBlank()&&value.length()<=max
                &&value.matches("[A-Za-z0-9_.$:/ \\[\\]-]+");}
    }
    public WorkflowSessionSnapshot restored(Freshness value,List<String> reasons){return new WorkflowSessionSnapshot(schemaVersion,workflowId,requirement,state,createdAt,updatedAt,projectFingerprint,sourceFingerprint,value,plan,implementation,execution,diagnosis,repair,repairDecision,repairApplyStatus,true,reasons,agentFailure,review,reviewHistory,runDiagnostic);}
    public WorkflowSessionSnapshot restoredAfterInterruption(Freshness value,List<String> reasons){
        State restoredState=switch(state){
            case PLAN_GENERATING,IMPLEMENTATION_GENERATING->State.AGENT_EXECUTION_INTERRUPTED;
            case EXECUTION_RUNNING,VERIFICATION_RUNNING->State.EXECUTION_INTERRUPTED;
            default->state;
        };
        RunDiagnosticSnapshot restoredDiagnostic=(state==State.EXECUTION_RUNNING||state==State.VERIFICATION_RUNNING)
                ?new RunDiagnosticSnapshot(RunReason.EXECUTION_RESULT_UNKNOWN,RunStatus.INTERRUPTED,
                state==State.VERIFICATION_RUNNING?"RERUN":"RUN",true):runDiagnostic;
        return new WorkflowSessionSnapshot(schemaVersion,workflowId,requirement,restoredState,createdAt,updatedAt,
                projectFingerprint,sourceFingerprint,value,plan,implementation,execution,diagnosis,repair,
                repairDecision,repairApplyStatus,true,reasons,agentFailure,review,reviewHistory,restoredDiagnostic);
    }
    public WorkflowSessionSnapshot withNewRepair(RepairProposal value,Instant changedAt){return new WorkflowSessionSnapshot(schemaVersion,workflowId,requirement,State.REPAIR_READY_FOR_REVIEW,createdAt,changedAt,projectFingerprint,sourceFingerprint,freshness,plan,implementation,execution,diagnosis,Objects.requireNonNull(value,"repair"),null,null,resumed,limitations,null,review,reviewHistory,runDiagnostic);}
    public List<String> availableActions(){
        if(resumed)return List.of();
        if(runDiagnostic!=null&&(runDiagnostic.reason()==RunReason.EXECUTION_RESULT_UNKNOWN
                ||runDiagnostic.reason()==RunReason.POST_EXECUTION_PROCESSING_FAILED
                ||runDiagnostic.reason()==RunReason.THREAD_INTERRUPTED
                ||runDiagnostic.reason()==RunReason.RECOVERY_PERSISTENCE_FAILED))return List.of();
        if(freshness!=Freshness.FRESH)return switch(state){case PLAN_READY_FOR_REVIEW,IMPLEMENTATION_READY_FOR_REVIEW,
                REPAIR_READY_FOR_REVIEW,REPAIR_APPLIED_VERIFICATION_PENDING,EXECUTION_INTERRUPTED->List.of("REGENERATE_PLAN");default->List.of();};
        return switch(state){
            case CREATED->List.of("GENERATE_PLAN");
            case AGENT_FAILED,AGENT_EXECUTION_INTERRUPTED->plan==null?List.of("GENERATE_PLAN"):List.of("GENERATE_IMPLEMENTATION");
            case PLAN_READY_FOR_REVIEW->List.of("GENERATE_IMPLEMENTATION");
            case IMPLEMENTATION_READY_FOR_REVIEW->List.of("RUN");
            case EXECUTION_FAILED->List.of("DIAGNOSE");
            case REPAIR_READY_FOR_REVIEW->List.of("DIAGNOSE","APPROVE_REPAIR","REJECT_REPAIR");
            case REPAIR_APPLIED_VERIFICATION_PENDING->List.of("RERUN");
            case EXECUTION_INTERRUPTED->repair!=null&&"APPROVED".equals(repairDecision)&&"APPLIED".equals(repairApplyStatus)
                    ?List.of("RERUN"):(implementation!=null?List.of("RUN"):List.of());
            default->List.of();
        };
    }

    public Map<String,Object> toDocument(){
        Map<String,Object> value=new LinkedHashMap<>();value.put("schemaVersion",schemaVersion);value.put("workflowId",workflowId);value.put("requirement",requirement);value.put("state",state.name());value.put("createdAt",createdAt.toString());value.put("updatedAt",updatedAt.toString());value.put("projectFingerprint",nullToEmpty(projectFingerprint));value.put("sourceFingerprint",nullToEmpty(sourceFingerprint));value.put("freshness",freshness.name());value.put("plan",projection(plan));value.put("implementation",projection(implementation));value.put("execution",projection(execution));value.put("diagnosis",projection(diagnosis));value.put("repair",projection(repair));value.put("repairDecision",nullToEmpty(repairDecision));value.put("repairApplyStatus",nullToEmpty(repairApplyStatus));value.put("resumed",resumed);value.put("limitations",limitations);value.put("agentFailure",projection(agentFailure));value.put("review",projection(review));value.put("reviewHistory",reviewHistory.stream().map(WorkflowSessionSnapshot::projection).toList());value.put("runDiagnostic",projection(runDiagnostic));return value;
    }
    public static WorkflowSessionSnapshot fromDocument(Map<String,Object> value){
        Set<String> legacyFields=Set.of("schemaVersion","workflowId","requirement","state","createdAt","updatedAt","projectFingerprint","sourceFingerprint","freshness","plan","implementation","execution","diagnosis","repair","repairDecision","repairApplyStatus","resumed","limitations");
        Set<String> v1Fields=new HashSet<>(legacyFields);v1Fields.add("agentFailure");
        Set<String> v2Fields=new HashSet<>(v1Fields);v2Fields.add("review");v2Fields.add("reviewHistory");
        Set<String> currentFields=new HashSet<>(v2Fields);currentFields.add("runDiagnostic");
        if(!value.keySet().equals(legacyFields)&&!value.keySet().equals(v1Fields)&&!value.keySet().equals(v2Fields)&&!value.keySet().equals(currentFields))throw new StrictJson.JsonFormatException("Workflow snapshot fields are invalid");
        int storedSchema=number(value,"schemaVersion");if(storedSchema!=SCHEMA_VERSION)throw new StrictJson.JsonFormatException("Unsupported workflow snapshot schemaVersion");
        return new WorkflowSessionSnapshot(SCHEMA_VERSION,text(value,"workflowId"),text(value,"requirement"),enumeration(value,"state",State.class),Instant.parse(text(value,"createdAt")),Instant.parse(text(value,"updatedAt")),emptyToNull(text(value,"projectFingerprint")),emptyToNull(text(value,"sourceFingerprint")),enumeration(value,"freshness",Freshness.class),decode(value,"plan",AgentExecutor.Role.TEST_ARCHITECT,"TEST_PLAN",TestPlan.class),decode(value,"implementation",AgentExecutor.Role.TEST_IMPLEMENTER,"TEST_IMPLEMENTATION_PROPOSAL",TestImplementationProposal.class),decode(value,"execution",AgentExecutor.Role.TEST_VERIFIER,"TEST_EXECUTION_RESULT",TestExecutionResult.class),decode(value,"diagnosis",AgentExecutor.Role.FAILURE_CLASSIFIER,"FAILURE_CLASSIFICATION",FailureClassification.class),decodeRepair(value),emptyToNull(text(value,"repairDecision")),emptyToNull(text(value,"repairApplyStatus")),Boolean.TRUE.equals(value.get("resumed")),strings(value,"limitations"),failure(value.get("agentFailure")),decodeReview(value.get("review")),reviewHistory(value.get("reviewHistory")),runDiagnostic(value.get("runDiagnostic")));
    }
    @SuppressWarnings("unchecked") private static AgentFailureSnapshot failure(Object value){if(!(value instanceof Map<?,?> raw)||raw.isEmpty())return null;Map<String,Object> map=(Map<String,Object>)raw;Set<String> fields=Set.of("code","stage","reasonCode","location","message","metadata");if(!map.keySet().equals(fields))throw new StrictJson.JsonFormatException("Agent failure fields are invalid");Object metadataValue=map.get("metadata");if(!(metadataValue instanceof Map<?,?> rawMetadata))throw new StrictJson.JsonFormatException("Agent failure metadata must be an object");Map<String,String> metadata=new TreeMap<>();for(var entry:rawMetadata.entrySet()){if(!(entry.getKey() instanceof String key)||!(entry.getValue() instanceof String text))throw new StrictJson.JsonFormatException("Agent failure metadata entries must be strings");metadata.put(key,text);}return new AgentFailureSnapshot(text(map,"code"),text(map,"stage"),text(map,"reasonCode"),text(map,"location"),text(map,"message"),metadata);}
    @SuppressWarnings("unchecked") private static RunDiagnosticSnapshot runDiagnostic(Object value){if(!(value instanceof Map<?,?> raw)||raw.isEmpty())return null;Map<String,Object> map=(Map<String,Object>)raw;Set<String> fields=Set.of("reason","status","location","recoveryPersisted");if(!map.keySet().equals(fields))throw new StrictJson.JsonFormatException("Run diagnostic fields are invalid");return new RunDiagnosticSnapshot(enumeration(map,"reason",RunReason.class),enumeration(map,"status",RunStatus.class),text(map,"location"),Boolean.TRUE.equals(map.get("recoveryPersisted")));}
    @SuppressWarnings("unchecked") private static CodeReviewResult decodeReview(Object value){
        if(!(value instanceof Map<?,?> raw)||raw.isEmpty())return null;
        Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("schemaVersion",AgentProtocolCodec.SCHEMA_VERSION);
        envelope.put("resultType","CODE_REVIEW_RESULT");envelope.put("payload",new LinkedHashMap<>((Map<String,Object>)raw));
        return (CodeReviewResult)new AgentProtocolCodec().decode(AgentExecutor.Role.CODE_REVIEWER,
                StrictJson.write(envelope)).payload();
    }
    @SuppressWarnings("unchecked") private static List<ReviewCycleSnapshot> reviewHistory(Object value){
        if(value==null)return List.of();if(!(value instanceof List<?> list)||list.size()>2)
            throw new StrictJson.JsonFormatException("reviewHistory must be a bounded array");
        List<ReviewCycleSnapshot> result=new ArrayList<>();for(Object item:list){
            if(!(item instanceof Map<?,?> raw))throw new StrictJson.JsonFormatException("reviewHistory entry must be an object");
            Map<String,Object> map=(Map<String,Object>)raw;
            Set<String> fields=Set.of("implementationVersion","implementationArtifactRef","executionArtifactRef",
                    "reviewArtifactRef","compileOutcome","executionOutcome","durationMillis","review");
            if(!map.keySet().equals(fields))throw new StrictJson.JsonFormatException("reviewHistory entry fields are invalid");
            result.add(new ReviewCycleSnapshot(number(map,"implementationVersion"),text(map,"implementationArtifactRef"),
                    text(map,"executionArtifactRef"),text(map,"reviewArtifactRef"),text(map,"compileOutcome"),
                    text(map,"executionOutcome"),longNumber(map,"durationMillis"),decodeReview(map.get("review"))));
        }return List.copyOf(result);
    }
    private static Object projection(Object value){return value==null?Map.of():StrictJson.readObject(StrictJson.write(value));}
    @SuppressWarnings("unchecked") private static <T>T decode(Map<String,Object> root,String name,AgentExecutor.Role role,String type,Class<T> expected){Object payload=root.get(name);if(!(payload instanceof Map<?,?> map)||map.isEmpty())return null;Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("schemaVersion",AgentProtocolCodec.SCHEMA_VERSION);envelope.put("resultType",type);envelope.put("payload",map);Object decoded=new AgentProtocolCodec().decode(role,StrictJson.write(envelope)).payload();return expected.cast(decoded);}
    @SuppressWarnings("unchecked") private static RepairProposal decodeRepair(Map<String,Object> root){Object payload=root.get("repair");if(!(payload instanceof Map<?,?> raw)||raw.isEmpty())return null;Map<String,Object> full=new LinkedHashMap<>((Map<String,Object>)raw);Map<String,Object> base=new LinkedHashMap<>(full);base.remove("sourceTarget");base.remove("replacementEvidence");base.remove("affectedMethods");Map<String,Object> envelope=new LinkedHashMap<>();envelope.put("schemaVersion",AgentProtocolCodec.SCHEMA_VERSION);envelope.put("resultType","REPAIR_PROPOSAL");envelope.put("payload",base);RepairProposal decoded=(RepairProposal)new AgentProtocolCodec().decode(AgentExecutor.Role.STABILIZER,StrictJson.write(envelope)).payload();RepairProposal.SourceTarget target=sourceTarget(full.get("sourceTarget"));RepairProposal.SelectorEvidence evidence=selectorEvidence(full.get("replacementEvidence"));return new RepairProposal(decoded.header(),decoded.proposalId(),decoded.applicationPolicy(),decoded.whatChanged(),decoded.why(),decoded.oldSelector(),decoded.newSelector(),decoded.sourceDeclarationRef(),decoded.oldCandidateId(),decoded.newCandidateId(),decoded.classificationRef(),decoded.driftRef(),decoded.sameTargetEvidence(),decoded.stabilityEvidence(),decoded.affectedTests(),decoded.risks(),decoded.verificationPlan(),target,evidence,stringsOrEmpty(full.get("affectedMethods"),"affectedMethods"));}
    @SuppressWarnings("unchecked") private static RepairProposal.SourceTarget sourceTarget(Object value){if(!(value instanceof Map<?,?> raw)||raw.isEmpty())return null;Map<String,Object> map=(Map<String,Object>)raw;Map<String,Object> range=(Map<String,Object>)map.get("range");return new RepairProposal.SourceTarget(text(map,"sourceElementId"),text(map,"declarationRef"),text(map,"logicalPath"),new RepairProposal.SourceRange(number(range,"startLine"),number(range,"startColumn"),number(range,"endLine"),number(range,"endColumn"),number(range,"startOffset"),number(range,"endOffsetExclusive")),text(map,"sourceFileFingerprint"),text(map,"declarationFingerprint"),text(map,"correlationState"),text(map,"oldStrategy"),text(map,"oldValue"));}
    @SuppressWarnings("unchecked") private static RepairProposal.SelectorEvidence selectorEvidence(Object value){if(!(value instanceof Map<?,?> raw)||raw.isEmpty())return null;Map<String,Object> map=(Map<String,Object>)raw;return new RepairProposal.SelectorEvidence(text(map,"strategy"),text(map,"value"),text(map,"candidateId"),text(map,"validation"),text(map,"sameTarget"),Boolean.TRUE.equals(map.get("unique")),stringsOrEmpty(map.get("stability"),"stability"),text(map,"source"));}
    private static String text(Map<String,Object> value,String name){Object item=value.get(name);if(!(item instanceof String text))throw new StrictJson.JsonFormatException(name+" must be a string");return text;}
    private static int number(Map<String,Object> value,String name){Object item=value.get(name);if(!(item instanceof Number number))throw new StrictJson.JsonFormatException(name+" must be a number");return number.intValue();}
    private static long longNumber(Map<String,Object> value,String name){Object item=value.get(name);if(!(item instanceof Number number))throw new StrictJson.JsonFormatException(name+" must be a number");return number.longValue();}
    private static <E extends Enum<E>>E enumeration(Map<String,Object> value,String name,Class<E> type){try{return Enum.valueOf(type,text(value,name));}catch(IllegalArgumentException failure){throw new StrictJson.JsonFormatException("Invalid "+name);}}
    private static List<String> strings(Map<String,Object> value,String name){Object item=value.get(name);if(!(item instanceof List<?> list))throw new StrictJson.JsonFormatException(name+" must be an array");return list.stream().map(entry->{if(!(entry instanceof String text))throw new StrictJson.JsonFormatException(name+" entries must be strings");return text;}).toList();}
    private static List<String> stringsOrEmpty(Object item,String name){if(item==null)return List.of();if(!(item instanceof List<?> list))throw new StrictJson.JsonFormatException(name+" must be an array");return list.stream().map(entry->{if(!(entry instanceof String text))throw new StrictJson.JsonFormatException(name+" entries must be strings");return text;}).toList();}
    private static String nullToEmpty(String value){return value==null?"":value;}private static String emptyToNull(String value){return value.isBlank()?null:value;}
}
