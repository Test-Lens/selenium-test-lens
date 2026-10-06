package io.github.testlens.application.tooling.ai;

import java.util.ArrayList;
import java.util.List;

/** Conservative deterministic classifier; selector instability requires explicit selector evidence. @since 0.5.0 */
public final class EvidenceFailureClassifier {
    public FailureClassification classify(Signals signals){
        List<String>evidence=new ArrayList<>(),counter=new ArrayList<>();FailureClassification.Category category;String cause;
        if(signals.compileFailed()){category=FailureClassification.Category.COMPILATION;cause="Targeted compilation failed";evidence.addAll(signals.compileEvidence());}
        else if(signals.authenticationFailed()){category=FailureClassification.Category.AUTHENTICATION;cause="Authentication precondition failed";evidence.addAll(signals.authenticationEvidence());}
        else if(signals.productMismatch()){category=FailureClassification.Category.PRODUCT_DEFECT;cause="Observed product result contradicts the expected outcome";evidence.addAll(signals.productEvidence());counter.addAll(signals.selectorEvidence());}
        else if(signals.correlatedSelectorFailure()&&!signals.selectorEvidence().isEmpty()){category=FailureClassification.Category.SELECTOR_INSTABILITY;cause="Correlated source selector failed with Selector Intelligence evidence";evidence.addAll(signals.selectorEvidence());}
        else if(signals.synchronizationEvidence()!=null&&!signals.synchronizationEvidence().isEmpty()){category=FailureClassification.Category.SYNCHRONIZATION;cause="Observed state became available outside the expected synchronization contract";evidence.addAll(signals.synchronizationEvidence());}
        else if(signals.pageObjectCapabilityMissing()){category=FailureClassification.Category.PAGE_OBJECT_CAPABILITY;cause="Required behavior is absent from the correlated Page Object API";evidence.addAll(signals.capabilityEvidence());}
        else {category=FailureClassification.Category.UNKNOWN;cause="Available evidence is insufficient for a supported classification";counter.add("No decisive compile, product, selector, synchronization, authentication, or capability evidence");}
        ContractHeader.Confidence confidence=category==FailureClassification.Category.UNKNOWN?ContractHeader.Confidence.UNKNOWN:ContractHeader.Confidence.OBSERVED;
        return new FailureClassification(new ContractHeader(ContractHeader.SCHEMA_VERSION,category==FailureClassification.Category.UNKNOWN?ContractHeader.Status.PARTIAL:ContractHeader.Status.READY,evidence,List.of(),confidence),category,cause,signals.affectedIds(),evidence,counter);
    }
    public record Signals(boolean compileFailed,List<String>compileEvidence,boolean authenticationFailed,List<String>authenticationEvidence,
                          boolean productMismatch,List<String>productEvidence,boolean correlatedSelectorFailure,List<String>selectorEvidence,
                          List<String>synchronizationEvidence,boolean pageObjectCapabilityMissing,List<String>capabilityEvidence,List<String>affectedIds){
        public Signals{compileEvidence=safe(compileEvidence);authenticationEvidence=safe(authenticationEvidence);productEvidence=safe(productEvidence);selectorEvidence=safe(selectorEvidence);synchronizationEvidence=safe(synchronizationEvidence);capabilityEvidence=safe(capabilityEvidence);affectedIds=safe(affectedIds);}
        private static List<String>safe(List<String>v){return v==null?List.of():List.copyOf(v);}
    }
}
