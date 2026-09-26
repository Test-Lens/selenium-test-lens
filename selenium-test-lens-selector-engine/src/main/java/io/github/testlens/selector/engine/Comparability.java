package io.github.testlens.selector.engine;

import java.util.List;

/** Deterministic comparison decision; chronology is deliberately not evidence. */
public final class Comparability {
    private Comparability(){}
    public static Result evaluate(RunDescriptor left,RunDescriptor right,ObservationEvidence.Domain domain,ComparisonMode mode){
        if(left==null||right==null)return result(State.UNKNOWN,"RUN_METADATA_MISSING");
        if(mode==ComparisonMode.NOT_COMPARABLE)return result(State.NOT_COMPARABLE,"SUBJECT_NOT_COMPARABLE");
        if(mode==ComparisonMode.EXPECTED_DYNAMIC_VALUE)return result(State.NOT_COMPARABLE,"EXPECTED_DYNAMIC_VALUE");
        if(domain==ObservationEvidence.Domain.DIFFERENT_DATASET)return result(State.NOT_COMPARABLE,"DIFFERENT_DATASET");
        if(domain==ObservationEvidence.Domain.DIFFERENT_BUILD)return result(State.UNKNOWN,"DIFFERENT_BUILD_IS_SEPARATE_EVIDENCE");
        if(!left.logicalTestIdentityRef().equals(right.logicalTestIdentityRef()))return result(State.UNKNOWN,"LOGICAL_TEST_IDENTITY_DIFFERS");
        if(left.datasetKeyDigest()==null||right.datasetKeyDigest()==null)return result(State.UNKNOWN,"DATASET_KEY_MISSING");
        if(!left.datasetKeyDigest().equals(right.datasetKeyDigest()))return result(State.NOT_COMPARABLE,"DATASET_KEY_DIFFERS");
        if(left.systemUnderTestRevision()==null||right.systemUnderTestRevision()==null)return result(State.UNKNOWN,"SUT_REVISION_MISSING");
        if(!left.systemUnderTestRevision().equals(right.systemUnderTestRevision()))return result(State.UNKNOWN,"SUT_REVISION_DIFFERS");
        if(domain==ObservationEvidence.Domain.NEW_RUN&&java.util.Objects.equals(left.invocationDiscriminator(),right.invocationDiscriminator())&&left.attempt()!=right.attempt())return result(State.UNKNOWN,"RETRY_IS_NOT_INDEPENDENT_RUN");
        return result(State.COMPARABLE,"SAME_LOGICAL_TEST_DATASET_AND_BUILD");
    }
    private static Result result(State state,String reason){return new Result(state,List.of(reason));}
    public enum State{COMPARABLE,NOT_COMPARABLE,UNKNOWN}
    public enum ComparisonMode{EXACT_VALUE,STRUCTURAL_COMPONENTS_ONLY,EXPECTED_DYNAMIC_VALUE,NOT_COMPARABLE}
    public record Result(State state,List<String> reasonCodes){public Result{reasonCodes=List.copyOf(reasonCodes);}}
}
