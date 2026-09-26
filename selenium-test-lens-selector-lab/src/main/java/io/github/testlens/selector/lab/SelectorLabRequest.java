package io.github.testlens.selector.lab;

import io.github.testlens.core.redaction.RedactionPolicy;
import io.github.testlens.selector.engine.*;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Explicit internal request. Constructing it performs no browser operation. */
public record SelectorLabRequest(WebDriver driver,SearchContext searchContext,CandidateAnalysis.UsageIntent usageIntent,
                                 By originalBy,String contextFingerprint,String declarationRef,String modulePath,
                                 String logicalPath,String declaringSymbol,String usageClass,String usageMethod,
                                 CompiledPolicySet policies,ObservationEvidence evidence,List<String>preferredTestAttributes,
                                 RedactionPolicy redactionPolicy,DisplayMode displayMode,SelectorAuditProjection auditProjection,
                                 List<SimilaritySubject>similarityCatalog,Map<String,ObservationEvidence>similarityEvidence,
                                 SimilarityResult.QueryScope similarityScope,boolean incompleteHistory,String preparedSourceNavigationTarget){
    public SelectorLabRequest{
        Objects.requireNonNull(driver,"driver");searchContext=searchContext==null?driver:searchContext;
        usageIntent=usageIntent==null?CandidateAnalysis.UsageIntent.UNKNOWN:usageIntent;
        policies=policies==null?CompiledPolicySet.empty():policies;evidence=evidence==null?ObservationEvidence.unavailable():evidence;
        preferredTestAttributes=preferredTestAttributes==null?List.of():List.copyOf(preferredTestAttributes);
        redactionPolicy=redactionPolicy==null?RedactionPolicy.defaults():redactionPolicy;displayMode=displayMode==null?DisplayMode.STANDARD:displayMode;
        similarityCatalog=similarityCatalog==null?List.of():List.copyOf(similarityCatalog);
        similarityEvidence=similarityEvidence==null?Map.of():Map.copyOf(similarityEvidence);
        similarityScope=similarityScope==null?SimilarityResult.QueryScope.PROJECT:similarityScope;
        if(preparedSourceNavigationTarget!=null&&!preparedSourceNavigationTarget.startsWith("jetbrains://idea/navigate/"))
            preparedSourceNavigationTarget=null;
    }
    public enum DisplayMode{STANDARD,DEBUG}
}
