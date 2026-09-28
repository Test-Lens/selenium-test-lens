package io.github.testlens.migration.tooling;

import java.util.List;
import java.util.Objects;

/** Immutable description of recovery actions that could be requested through existing trusted services. */
public record MigrationRecoveryActionPlan(String actionPlanId,String transactionId,MigrationRecoveryPlan.Situation situation,
        List<Action>actions,List<String>requiredCapabilities,List<String>limitations){
    public MigrationRecoveryActionPlan{MigrationApplyPlan.require(transactionId,"migration-apply-transaction-v1");Objects.requireNonNull(situation);actions=actions==null?List.of():actions.stream().distinct().sorted().toList();requiredCapabilities=strings(requiredCapabilities);limitations=strings(limitations);String expected=id(transactionId,situation,actions,requiredCapabilities,limitations);if(!expected.equals(actionPlanId))throw new IllegalArgumentException("actionPlanId");}
    public static MigrationRecoveryActionPlan from(MigrationRecoveryPlan plan){List<Action>actions=switch(plan.situation()){case SAFE_TO_ABANDON->List.of(Action.ABANDON_TRANSACTION,Action.CLEAN_REGISTERED_TEMPS);case ALL_APPLIED_NEEDS_RECONCILIATION->List.of(Action.RECONCILE_APPLIED_STATE);case GUARDED_ROLLBACK_AVAILABLE->List.of(Action.ROLLBACK_TOOL_CHANGES);case ROLLBACK_PARTIALLY_BLOCKED->List.of(Action.ROLLBACK_TOOL_CHANGES,Action.MANUAL_RECOVERY);case RECOVERY_REQUIRED->List.of(Action.MANUAL_RECOVERY);};List<String>caps=actions.contains(Action.ROLLBACK_TOOL_CHANGES)?List.of(MigrationTargetedAuthorization.Capability.ROLLBACK_TOOL_CHANGES.name()):List.of();return new MigrationRecoveryActionPlan(id(plan.transactionId(),plan.situation(),actions,caps,plan.issues()),plan.transactionId(),plan.situation(),actions,caps,plan.issues());}
    public static MigrationRecoveryActionPlan create(String transaction,MigrationRecoveryPlan.Situation situation,List<Action>actions,List<String>capabilities,List<String>limitations){return new MigrationRecoveryActionPlan(id(transaction,situation,actions,capabilities,limitations),transaction,situation,actions,capabilities,limitations);}
    public enum Action{ABANDON_TRANSACTION,CLEAN_REGISTERED_TEMPS,RECONCILE_APPLIED_STATE,ROLLBACK_TOOL_CHANGES,MANUAL_RECOVERY}
    private static String id(String tx,MigrationRecoveryPlan.Situation s,List<Action>a,List<String>caps,List<String>limits){return MigrationVerificationPlan.ref("migration-recovery-action-plan-v1",tx,s.name(),String.join("|",a.stream().map(Enum::name).sorted().toList()),String.join("|",strings(caps)),String.join("|",strings(limits)));}private static List<String>strings(List<String>v){return v==null?List.of():v.stream().filter(Objects::nonNull).distinct().sorted().toList();}
}
